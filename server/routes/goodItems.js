const express = require('express');
const { auth, checkBanned } = require('../middleware/auth');
const GoodItem = require('../models/GoodItem');
const User = require('../models/User');

const router = express.Router();

// 加载违禁词库（从 JSON 文件读取，方便手动管理）
const SENSITIVE_WORDS = require('../config/sensitive-words.json').words;

function containsSensitiveWords(text) {
  if (!text) return false;
  const lowerText = text.toLowerCase();
  return SENSITIVE_WORDS.some(word => lowerText.includes(word.toLowerCase()));
}

// 发布好物（封禁用户禁止发帖）
router.post('/', auth, checkBanned, async (req, res) => {
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
router.get('/', auth, async (req, res) => {
  try {
    const { contentType, category, sort = 'newest', page = 1, limit = 20, author, status, favorites } = req.query;

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

    // 收藏查询：需要登录，返回当前用户收藏的帖子
    if (favorites === 'true') {
      const user = await User.findById(req.userId);
      const favoriteIds = (user && user.favorites) ? user.favorites.map(id => id.toString()) : [];
      if (favoriteIds.length === 0) {
        // 没有收藏，直接返回空数组
        return res.json([]);
      }
      query._id = { $in: favoriteIds };
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

    // 查询当前用户的收藏集合，用于 isFavorited 判断
    const currentUser = await User.findById(req.userId);
    const favSet = currentUser && currentUser.favorites
      ? new Set(currentUser.favorites.map(id => id.toString()))
      : new Set();

    res.json(goodItems.map(item => formatGoodItem(item, req.userId, favSet)));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取好物详情
router.get('/:id', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id)
      .populate('author', 'username nickname avatar')
      .populate('comments.user', 'username nickname avatar');

    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    // 查询当前用户的收藏集合
    const currentUser = await User.findById(req.userId);
    const favSet = currentUser && currentUser.favorites
      ? new Set(currentUser.favorites.map(id => id.toString()))
      : new Set();

    res.json(formatGoodItem(goodItem, req.userId, favSet));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 收藏好物
router.post('/:id/favorite', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    const user = await User.findById(req.userId);
    if (!user.favorites) user.favorites = [];
    
    const alreadyFavorited = user.favorites.some(id => id.toString() === req.params.id);
    if (!alreadyFavorited) {
      user.favorites.push(req.params.id);
      await user.save();
    }

    // 收藏后，该 item 对当前用户一定是已收藏状态
    const favSet = new Set(user.favorites.map(id => id.toString()));
    res.json(formatGoodItem(goodItem, req.userId, favSet));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 取消收藏
router.delete('/:id/favorite', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    const user = await User.findById(req.userId);
    if (user.favorites) {
      user.favorites = user.favorites.filter(id => id.toString() !== req.params.id);
      await user.save();
    }

    // 取消收藏后，该 item 对当前用户一定不是已收藏状态
    const favSet = new Set((user.favorites || []).map(id => id.toString()));
    res.json(formatGoodItem(goodItem, req.userId, favSet));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 修改好物（封禁用户禁止修改）
router.put('/:id', auth, checkBanned, async (req, res) => {
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

// 删除好物（封禁用户禁止删除）
router.delete('/:id', auth, checkBanned, async (req, res) => {
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

// 点赞（封禁用户禁止点赞）
router.post('/:id/like', auth, checkBanned, async (req, res) => {
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

    res.json(formatGoodItem(updatedItem, req.userId));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 评论（封禁用户禁止评论）
router.post('/:id/comment', auth, checkBanned, async (req, res) => {
  try {
    const { content, parentId } = req.body;
    if (!content || content.trim().length === 0) {
      return res.status(400).json({ message: '评论内容不能为空' });
    }

    const trimmedContent = content.trim();

    // 违禁词检测
    if (containsSensitiveWords(trimmedContent)) {
      return res.status(400).json({ message: '评论包含违规内容，禁止发布' });
    }

    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    // 如果指定了 parentId，验证父评论存在
    if (parentId) {
      const parentComment = goodItem.comments.id(parentId);
      if (!parentComment) {
        return res.status(404).json({ message: '回复的评论不存在' });
      }
    }

    goodItem.comments.push({
      user: req.userId,
      content: trimmedContent,
      parentId: parentId || null
    });

    await goodItem.save();
    await goodItem.populate('comments.user', 'username nickname avatar');

    res.status(201).json(formatGoodItem(goodItem, req.userId));
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

// 评论点赞/取消（封禁用户禁止点赞）
router.post('/:id/comment/:commentId/like', auth, checkBanned, async (req, res) => {
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

    res.json(formatGoodItem(updated, req.userId));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 删除评论（只能删除自己的评论）
router.delete('/:id/comment/:commentId', auth, async (req, res) => {
  try {
    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) return res.status(404).json({ message: '帖子不存在' });

    const comment = goodItem.comments.id(req.params.commentId);
    if (!comment) return res.status(404).json({ message: '评论不存在' });

    // 只能删除自己的评论
    if (comment.user.toString() !== req.userId) {
      return res.status(403).json({ message: '无权删除他人评论' });
    }

    goodItem.comments.pull({ _id: req.params.commentId });
    await goodItem.save();

    const updated = await GoodItem.findById(req.params.id)
      .populate('author', 'username nickname avatar')
      .populate('comments.user', 'username nickname avatar');

    res.json(formatGoodItem(updated, req.userId));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 用户提交审核（封禁用户禁止提交）
router.put('/:id/submit-review', auth, checkBanned, async (req, res) => {
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
function formatGoodItem(item, currentUserId = null, favSet = null) {
  const result = {
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
    comments: buildCommentTree(item.comments),
    commentsCount: item.comments.length,
    createdAt: item.createdAt?.getTime() || Date.now()
  };

  // 如果传入了收藏集合，检查是否已收藏
  if (favSet && currentUserId) {
    result.isFavorited = favSet.has(item._id.toString());
  }

  return result;
}

function buildCommentTree(comments) {
  const all = comments.map(c => ({
    id: c._id.toString(),
    content: c.content,
    likesCount: c.likes?.length || 0,
    likedByMe: false,
    parentId: c.parentId ? c.parentId.toString() : null,
    createdAt: c.createdAt?.getTime() || Date.now(),
    user: c.user ? {
      id: c.user._id?.toString(),
      username: c.user.username,
      nickname: c.user.nickname,
      avatar: c.user.avatar
    } : null,
    replies: []
  }));

  const map = new Map();
  all.forEach(c => map.set(c.id, c));

  const roots = [];
  all.forEach(c => {
    if (c.parentId && map.has(c.parentId)) {
      map.get(c.parentId).replies.push(c);
    } else {
      roots.push(c);
    }
  });

  // 按时间排序回复
  roots.forEach(c => c.replies.sort((a, b) => a.createdAt - b.createdAt));

  return roots;
}

module.exports = router;
