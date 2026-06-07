const express = require('express');
const auth = require('../middleware/auth');
const GoodItem = require('../models/GoodItem');

const router = express.Router();

// 发布好物
router.post('/', auth, async (req, res) => {
  try {
    const { title, description, category, subCategory, images, link } = req.body;

    const goodItem = new GoodItem({
      title,
      description,
      category: category.toLowerCase(),
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
    const { category, sort = 'newest', page = 1, limit = 20, author } = req.query;

    const query = {};
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
    const { title, description, category, subCategory, images, link } = req.body;

    const goodItem = await GoodItem.findById(req.params.id);
    if (!goodItem) {
      return res.status(404).json({ message: '好物不存在' });
    }

    if (goodItem.author.toString() !== req.userId) {
      return res.status(403).json({ message: '无权修改此好物' });
    }

    goodItem.title = title || goodItem.title;
    goodItem.description = description !== undefined ? description : goodItem.description;
    goodItem.category = category ? category.toLowerCase() : goodItem.category;
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
    res.json(formatGoodItem(goodItem));
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

// 格式化好物数据
function formatGoodItem(item) {
  return {
    id: item._id.toString(),
    title: item.title,
    description: item.description,
    category: item.category.toUpperCase(),
    subCategory: item.subCategory,
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
    comments: item.comments.map(c => ({
      id: c._id.toString(),
      content: c.content,
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
