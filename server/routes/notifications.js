const express = require('express');
const auth = require('../middleware/auth');
const Notification = require('../models/Notification');

const router = express.Router();

// 获取未读数量
router.get('/unread-count', auth, async (req, res) => {
  try {
    const count = await Notification.countDocuments({ recipient: req.userId, read: false });
    res.json({ count });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取通知列表
router.get('/', auth, async (req, res) => {
  try {
    const { page = 1, limit = 30 } = req.query;
    const notifications = await Notification.find({ recipient: req.userId })
      .sort({ createdAt: -1 })
      .skip((page - 1) * limit)
      .limit(parseInt(limit))
      .lean();

    const total = await Notification.countDocuments({ recipient: req.userId });

    res.json({
      notifications: notifications.map(n => ({
        id: n._id.toString(),
        type: n.type,
        title: n.title,
        message: n.message,
        relatedPostId: n.relatedPostId?.toString() || null,
        extra: n.extra || null,
        read: n.read,
        createdAt: n.createdAt?.getTime() || Date.now()
      })),
      total,
      unread: await Notification.countDocuments({ recipient: req.userId, read: false })
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 标为已读
router.put('/:id/read', auth, async (req, res) => {
  try {
    await Notification.findOneAndUpdate(
      { _id: req.params.id, recipient: req.userId },
      { read: true }
    );
    res.json({ message: 'ok' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 全部标为已读
router.put('/read-all', auth, async (req, res) => {
  try {
    await Notification.updateMany(
      { recipient: req.userId, read: false },
      { read: true }
    );
    res.json({ message: 'ok' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;
