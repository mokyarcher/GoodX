const express = require('express');
const auth = require('../middleware/auth');
const GoodItem = require('../models/GoodItem');

const router = express.Router();

// 发布好物
router.post('/', auth, async (req, res) => {
  try {
    const { title, description, contentType, category, subCategory, images, link } = req.body;
    const normalizedCategory = category.toLowerCase();

    const goodItem = new GoodItem({
      title,
      description,
      contentType: normalizeContentType(contentType, normalizedCategory),
      category: normalizedCategory,
      subCategory,
      images: images || [],
      link,
      author: req.userId
    });

    await goodItem.save();
    await goodItem.populate('author', 'username nickname avatar');

    res.status(201).json(formatGoodItem(goodItem));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取好物列表（支持按品类筛选）
router.get('/', async (req, res) => {
  try {
    const { contentType, category, sort = 'newest', page = 1, limit = 20, author, status } = req.query;

    const query = {};
    // 默认只返回 active 的帖子，管理员可通过 status=all 查看全部
    if (status === 'all') {
      // 不做 status 过滤
    } else if (status === 'removed') {
      query.status = { $in: ['removed', 'pending_review'] };
    } else {
      query.status = 'active';
    }
    if (contentType) {
      query.contentType = contentType.toLowerCase();
    }
    if (category) {
      query.category = category.toLowerCase();
    }
    if (author) {
      query.author = author;
    }

    let sortOption = {};
    if (sort === 'newest') {
      sortOption = { createdAt: -1 };
    } else if (sort === 'popular') {
      sortOption = { likes: -1 };
    }

    const goodItems = await GoodItem.find(query)
      .populate('author', 'username nickname avatar')
      .populate('comments.user', 'username nickname avatar')
      .populate('likes', 'username nickname')
      .sort(sortOption)
      .skip((page - 1) * limit)
      .limit(parseInt(limit));

    res.json(goodItems.map(formatGoodItem));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取好物详情
router.get('/:id', async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id)
      .populate('author', 'username nickname avatar')
      .populate('comments.user', 'username nickname avatar');

    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    res.json(formatGoodItem(goodItem));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 修改好物
router.put('/:id', auth, async (req, res) => {
  try {
    const { title, description, contentType, category, subCategory, images, link } = req.body;

    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    if (goodItem.author.toString() !== req.userId) {
      return res.status(403).json({ message: '无权修改此好物' });
    }

    goodItem.title = title || goodItem.title;
    goodItem.description = description !== undefined ? description : goodItem.description;
    if (category) {
      goodItem.category = category.toLowerCase();
    }
    if (contentType || category) {
      goodItem.contentType = normalizeContentType(contentType, goodItem.category);
    }
    goodItem.subCategory = subCategory !== undefined ? subCategory : goodItem.subCategory;
    goodItem.images = images || goodItem.images;
    goodItem.link = link !== undefined ? link : goodItem.link;

    await goodItem.save();
    await goodItem.populate('author', 'username nickname avatar');

    res.json(formatGoodItem(goodItem));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 删除好物
router.delete('/:id', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    if (goodItem.author.toString() !== req.userId) {
      return res.status(403).json({ message: '无权删除此好物' });
    }

    await GoodItem.findByIdAndDelete(req.params.id);
    res.json({ message: '删除成功' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 点赞
router.post('/:id/like', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    const hasLiked = goodItem.likes.includes(req.userId);
    if (hasLiked) {
      goodItem.likes.pull(req.userId);
    } else {
      goodItem.likes.push(req.userId);
    }

    await goodItem.save();

    const updatedItem = await GoodItem.findById(req.params.id)
      .populate('author', 'username nickname avatar')
      .populate('comments.user', 'username nickname avatar');

    res.json(formatGoodItem(updatedItem));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 评论
router.post('/:id/comment', auth, async (req, res) => {
  try {
    const { content } = req.body;
    if (!content || content.trim().length === 0) {
      return res.status(400).json({ message: '评论内容不能为空' });
    }

    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    goodItem.comments.push({
      user: req.userId,
      content: content.trim()
    });

    await goodItem.save();
    await goodItem.populate('comments.user', 'username nickname avatar');

    res.status(201).json(formatGoodItem(goodItem));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

function normalizeContentType(contentType, category) {
  const normalized = contentType ? contentType.toLowerCase() : null;
  const normalizedCategory = (category || '').toLowerCase();

  if (['scenery', 'city', 'travel', 'daily', 'place', 'other_moments'].includes(normalizedCategory)) {
    return 'moments';
  }
  if (['movie', 'series', 'music', 'book', 'game', 'anime', 'podcast', 'other_entertainment'].includes(normalizedCategory)) {
    return 'entertainment';
  }
  if (['goods', 'moments', 'entertainment'].includes(normalized)) {
    return normalized;
  }
  return 'goods';
}

function latestInteraction(item) {
  const lastComment = item.comments[item.comments.length - 1];
  if (lastComment && lastComment.user) {
    return {
      type: 'comment',
      user: {
        nickname: lastComment.user.nickname || lastComment.user.username
      }
    };
  }
  const lastLike = item.likes[item.likes.length - 1];
  if (lastLike && lastLike._id) {
    return {
      type: 'like',
      user: {
        nickname: lastLike.nickname || lastLike.username
      }
    };
  }
  return null;
}

// 评论点赞/取消
router.post('/:id/comment/:commentId/like', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) return res.status(404).json({ message: '帖子不存在' });

    const comment = goodItem.comments.id(req.params.commentId);
    if (!comment) return res.status(404).json({ message: '评论不存在' });

    const idx = comment.likes.findIndex(uid => uid.toString() === req.userId);
    if (idx >= 0) comment.likes.splice(idx, 1);
    else comment.likes.push(req.userId);

    await goodItem.save();

    const updated = await GoodItem.findById(req.params.id)
      .populate('author', 'username nickname avatar')
      .populate('comments.user', 'username nickname avatar');

    res.json(formatGoodItem(updated));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 用户提交审核（编辑后申请重新上架）
router.put('/:id/submit-review', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) return res.status(404).json({ message: '帖子不存在' });
    if (goodItem.author.toString() !== req.userId) {
      return res.status(403).json({ message: '无权操作' });
    }
    if (goodItem.status !== 'removed') {
      return res.status(400).json({ message: '该帖子未被下架或已在审核中' });
    }

    // 对比快照，检查是否有实际修改
    const currentSnapshot = JSON.stringify({
      title: goodItem.title,
      description: goodItem.description,
      images: goodItem.images,
      category: goodItem.category,
      subCategory: goodItem.subCategory
    });

    if (goodItem.removedSnapshot && currentSnapshot === goodItem.removedSnapshot) {
      return res.status(400).json({ message: '未做任何修改，请先编辑内容后再提交审核' });
    }

    goodItem.status = 'pending_review';
    goodItem.removedSnapshot = null;
    await goodItem.save();

    // 通知所有管理员
    const User = require('../models/User');
    const Notification = require('../models/Notification');
    const adminUsers = await User.find({ isAdmin: true });
    const author = await User.findById(goodItem.author);

    for (const admin of adminUsers) {
      await Notification.create({
        recipient: admin._id,
        type: 'system',
        title: '有帖子申请重新上架',
        message: `${author?.nickname || author?.username || '用户'} 申请重新上架「${goodItem.title}」。\n请前往后台审核。`,
        relatedPostId: goodItem._id,
        extra: JSON.stringify({
          authorId: goodItem.author.toString(),
          authorName: author?.nickname || author?.username || '用户',
          action: 'review_posts'
        })
      });
    }

    res.json({ message: '已提交审核，等待管理员处理' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 格式化好物数据
function formatGoodItem(item) {
  return {
    id: item._id.toString(),
    title: item.title,
    description: item.description,
    contentType: normalizeContentType(item.contentType, item.category).toUpperCase(),
    category: item.category.toUpperCase(),
    subCategory: item.subCategory,
    status: item.status || 'active',
    removeReason: item.removeReason || null,
    images: item.images || [],
    link: item.link,
    author: item.author ? {
      id: item.author._id.toString(),
      username: item.author.username,
      nickname: item.author.nickname,
      avatar: item.author.avatar
    } : null,
    likes: item.likes.length,
    likedBy: item.likes.map(id => id.toString()),
    latestInteraction: latestInteraction(item),
    comments: item.comments.map(c => ({
      id: c._id.toString(),
      content: c.content,
      likesCount: c.likes?.length || 0,
      likedByMe: false, // 由客户端决定
      createdAt: c.createdAt?.getTime() || Date.now(),
      user: c.user ? {
        id: c.user._id?.toString(),
        username: c.user.username,
        nickname: c.user.nickname,
        avatar: c.user.avatar
      } : null
    })),
    commentsCount: item.comments.length,
    createdAt: item.createdAt?.getTime() || Date.now()
  };
}

module.exports = router;
