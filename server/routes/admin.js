const express = require('express');
const bcrypt = require('bcryptjs');
const auth = require('../middleware/auth');
const User = require('../models/User');

const router = express.Router();

// 管理员中间件
async function adminOnly(req, res, next) {
  try {
    const user = await User.findById(req.userId);
    if (!user || !user.isAdmin) {
      return res.status(403).json({ message: '无权访问' });
    }
    next();
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
}

// 确保管理员账号存在
async function ensureAdmin() {
  const admin = await User.findOne({ username: 'admin' });
  if (!admin) {
    const hashedPassword = await bcrypt.hash('Mqm112358', 10);
    await User.create({
      username: 'admin',
      password: hashedPassword,
      nickname: '管理员',
      isAdmin: true
    });
    console.log('Admin account created');
  }
}
ensureAdmin();

// 获取所有用户
router.get('/users', auth, adminOnly, async (req, res) => {
  try {
    const users = await User.find({}, { password: 0 });
    res.json(users.map(u => ({
      id: u._id.toString(),
      username: u.username,
      nickname: u.nickname,
      avatar: u.avatar,
      isAdmin: u.isAdmin,
      banned: u.banned,
      createdAt: u.createdAt?.getTime() || Date.now()
    })));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 修改用户
router.put('/users/:id', auth, adminOnly, async (req, res) => {
  try {
    const { nickname, password, banned } = req.body;
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ message: '用户不存在' });

    if (nickname !== undefined) user.nickname = nickname;
    if (banned !== undefined) user.banned = banned;
    if (password && password.length >= 6) {
      user.password = await bcrypt.hash(password, 10);
    }

    await user.save();
    res.json({ message: '修改成功' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 删除用户
router.delete('/users/:id', auth, adminOnly, async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ message: '用户不存在' });
    if (user.isAdmin) return res.status(400).json({ message: '不能删除管理员' });

    await User.findByIdAndDelete(req.params.id);
    res.json({ message: '删除成功' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取某用户的所有帖子（含已下架）
router.get('/users/:id/posts', auth, adminOnly, async (req, res) => {
  try {
    const GoodItem = require('../models/GoodItem');
    const items = await GoodItem.find({ author: req.params.id })
      .sort({ createdAt: -1 })
      .lean();
    res.json(items.map(item => ({
      id: item._id.toString(),
      title: item.title,
      description: item.description,
      category: item.category?.toUpperCase(),
      contentType: item.contentType || 'goods',
      images: item.images || [],
      status: item.status || 'active',
      removeReason: item.removeReason || null,
      likes: item.likes?.length || 0,
      commentsCount: item.comments?.length || 0,
      createdAt: item.createdAt?.getTime() || Date.now()
    })));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 管理员下架帖子（需填写理由，自动发送通知）
router.put('/posts/:id/remove', auth, adminOnly, async (req, res) => {
  try {
    const GoodItem = require('../models/GoodItem');
    const Notification = require('../models/Notification');
    const { reason } = req.body;
    if (!reason || !reason.trim()) {
      return res.status(400).json({ message: '请填写下架理由' });
    }
    const item = await GoodItem.findById(req.params.id);
    if (!item) return res.status(404).json({ message: '帖子不存在' });

    // 保存内容快照（标题+描述+图片+品类）
    const snapshot = JSON.stringify({
      title: item.title,
      description: item.description,
      images: item.images,
      category: item.category,
      subCategory: item.subCategory
    });

    item.status = 'removed';
    item.removeReason = reason.trim();
    item.removedSnapshot = snapshot;
    await item.save();
    if (!item) return res.status(404).json({ message: '帖子不存在' });

    // 发送系统通知给作者
    await Notification.create({
      recipient: item.author,
      type: 'system',
      title: '你的帖子已被下架',
      message: `「${item.title}」被管理员下架。理由：${reason.trim()}\n请编辑整改后提交审核。`,
      relatedPostId: item._id
    });

    res.json({ message: '已下架' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 管理员审核通过（重新上架）
router.put('/posts/:id/approve', auth, adminOnly, async (req, res) => {
  try {
    const GoodItem = require('../models/GoodItem');
    const Notification = require('../models/Notification');
    const item = await GoodItem.findByIdAndUpdate(
      req.params.id,
      { status: 'active', removeReason: null, removedSnapshot: null },
      { new: false }
    );
    if (!item) return res.status(404).json({ message: '帖子不存在' });

    await Notification.create({
      recipient: item.author,
      type: 'system',
      title: '审核通过，帖子已重新上架',
      message: `你的帖子「${item.title}」已通过管理员审核，已重新上架到公共区域。`,
      relatedPostId: item._id
    });

    res.json({ message: '已通过审核并上架' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 管理员拒绝重新上架
router.put('/posts/:id/reject', auth, adminOnly, async (req, res) => {
  try {
    const GoodItem = require('../models/GoodItem');
    const Notification = require('../models/Notification');
    const item = await GoodItem.findByIdAndUpdate(
      req.params.id,
      { status: 'removed', removedSnapshot: null },
      { new: false }
    );
    if (!item) return res.status(404).json({ message: '帖子不存在' });

    await Notification.create({
      recipient: item.author,
      type: 'system',
      title: '审核未通过，请继续修改',
      message: `你的帖子「${item.title}」未通过管理员审核，请继续编辑整改后重新提交。`,
      relatedPostId: item._id
    });

    res.json({ message: '已拒绝，退回修改' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 管理员删除帖子
router.delete('/posts/:id', auth, adminOnly, async (req, res) => {
  try {
    const GoodItem = require('../models/GoodItem');
    await GoodItem.findByIdAndDelete(req.params.id);
    res.json({ message: '已删除' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 检查当前用户是否为管理员
router.get('/check', auth, async (req, res) => {
  const user = await User.findById(req.userId);
  res.json({ isAdmin: user?.isAdmin || false });
});

module.exports = router;
