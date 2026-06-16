const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

let sharp = null;
try {
  sharp = require('sharp');
} catch (error) {
  console.warn('sharp is not installed, thumbnail generation will fall back to original images');
}

const router = express.Router();

// 确保上传目录存在
const uploadDir = path.join(__dirname, '../uploads');
const thumbsDir = path.join(uploadDir, 'thumbs');
const previewsDir = path.join(uploadDir, 'previews');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}
if (!fs.existsSync(thumbsDir)) {
  fs.mkdirSync(thumbsDir, { recursive: true });
}
if (!fs.existsSync(previewsDir)) {
  fs.mkdirSync(previewsDir, { recursive: true });
}

function thumbnailFilename(filename) {
  return `${path.parse(filename).name}-360.webp`;
}

function previewFilename(filename) {
  return `${path.parse(filename).name}-1280.webp`;
}

async function ensureThumbnail(filename) {
  const safeFilename = path.basename(filename);
  const sourcePath = path.join(uploadDir, safeFilename);
  const targetName = thumbnailFilename(safeFilename);
  const targetPath = path.join(thumbsDir, targetName);

  if (!fs.existsSync(sourcePath)) {
    return null;
  }

  if (fs.existsSync(targetPath)) {
    return targetPath;
  }

  if (!sharp) {
    return sourcePath;
  }

  await sharp(sourcePath)
    .rotate()
    .resize(360, 360, {
      fit: 'cover',
      position: 'centre',
      withoutEnlargement: true
    })
    .webp({ quality: 76, effort: 2 })
    .toFile(targetPath);

  return targetPath;
}

async function ensurePreview(filename) {
  const safeFilename = path.basename(filename);
  const sourcePath = path.join(uploadDir, safeFilename);
  const targetName = previewFilename(safeFilename);
  const targetPath = path.join(previewsDir, targetName);

  if (!fs.existsSync(sourcePath)) {
    return null;
  }

  if (fs.existsSync(targetPath)) {
    return targetPath;
  }

  if (!sharp) {
    return sourcePath;
  }

  await sharp(sourcePath)
    .rotate()
    .resize({
      width: 1280,
      height: 1280,
      fit: 'inside',
      withoutEnlargement: true
    })
    .webp({ quality: 86, effort: 2 })
    .toFile(targetPath);

  return targetPath;
}

// 存储配置
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname).toLowerCase() || '.jpg';
    cb(null, 'goodx-' + uniqueSuffix + ext);
  }
});

// 文件过滤 - 放宽限制，同时检查 mimetype 和扩展名
const fileFilter = (req, file, cb) => {
  const allowedMimeTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/jpg'];
  const allowedExts = ['.jpg', '.jpeg', '.png', '.webp', '.gif'];
  const ext = path.extname(file.originalname).toLowerCase();

  if (allowedMimeTypes.includes(file.mimetype) || allowedExts.includes(ext)) {
    cb(null, true);
  } else {
    cb(new Error('只支持 JPG/PNG/WebP/GIF 格式图片，当前类型: ' + file.mimetype), false);
  }
};

// 限制 5MB
const upload = multer({
  storage,
  fileFilter,
  limits: { fileSize: 20 * 1024 * 1024 }
});

// 错误处理中间件
const handleUploadError = (err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({ message: '图片大小不能超过 5MB' });
    }
    return res.status(400).json({ message: '上传错误: ' + err.message });
  } else if (err) {
    return res.status(400).json({ message: err.message });
  }
  next();
};

// 缩略图。发现页等列表场景使用。
router.get('/thumb/:filename', async (req, res) => {
  try {
    const safeFilename = path.basename(req.params.filename);
    const thumbnailPath = await ensureThumbnail(safeFilename);

    if (!thumbnailPath) {
      return res.status(404).json({ message: '图片不存在' });
    }

    res.set('Cache-Control', 'public, max-age=31536000, immutable');
    res.sendFile(thumbnailPath);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 预览图。详情页和全屏默认使用，用户主动点击后再加载原图。
router.get('/preview/:filename', async (req, res) => {
  try {
    const safeFilename = path.basename(req.params.filename);
    const previewPath = await ensurePreview(safeFilename);

    if (!previewPath) {
      return res.status(404).json({ message: '图片不存在' });
    }

    res.set('Cache-Control', 'public, max-age=31536000, immutable');
    res.sendFile(previewPath);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 单图上传
router.post('/image', upload.single('image'), handleUploadError, async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: '没有上传文件' });
    }

    // 上传时后台异步生成压缩图，不阻塞响应；首次访问 thumb/preview 接口时也会按需生成。
    Promise.all([
      ensureThumbnail(req.file.filename).catch(() => null),
      ensurePreview(req.file.filename).catch(() => null)
    ]).catch(() => {});

    const fileUrl = `/uploads/${req.file.filename}`;
    const thumbnailUrl = `/api/upload/thumb/${req.file.filename}`;
    const previewUrl = `/api/upload/preview/${req.file.filename}`;
    res.json({
      url: fileUrl,
      thumbnailUrl,
      previewUrl,
      filename: req.file.filename,
      size: req.file.size
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 多图上传（最多5张）
router.post('/images', upload.array('images', 5), handleUploadError, async (req, res) => {
  try {
    if (!req.files || req.files.length === 0) {
      return res.status(400).json({ message: '没有上传文件' });
    }

    Promise.all(req.files.flatMap(file => [
      ensureThumbnail(file.filename).catch(() => null),
      ensurePreview(file.filename).catch(() => null)
    ])).catch(() => {});

    const urls = req.files.map(file => `/uploads/${file.filename}`);
    const thumbnailUrls = req.files.map(file => `/api/upload/thumb/${file.filename}`);
    const previewUrls = req.files.map(file => `/api/upload/preview/${file.filename}`);
    res.json({
      urls,
      thumbnailUrls,
      previewUrls,
      count: req.files.length
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;
