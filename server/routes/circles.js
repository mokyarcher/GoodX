const express = require('express');
const { v4: uuidv4 } = require('uuid');
const { auth, checkBanned } = require('../middleware/auth');
const Circle = require('../models/Circle');
const User = require('../models/User');

const router = express.Router();

// 创建圈子（封禁用户禁止创建）
router.post('/', auth, checkBanned, async (req, res) => {
  try {
    const { name, description, maxMembers } = req.body;

    const circle = new Circle({
      name,
      description,
      owner: req.userId,
      inviteCode: uuidv4().substring(0, 8).toUpperCase(),
      members: [{ user: req.userId, role: 'owner' }],
      maxMembers: maxMembers || 50
    });

    await circle.save();

    await User.findByIdAndUpdate(req.userId, {
      $push: { circles: circle._id }
    });

    res.status(201).json(formatCircle(circle));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取我的圈子列表 (兼容前端 GET /)
router.get('/', auth, async (req, res) => {
  try {
    const user = await User.findById(req.userId).populate('circles');
    const circles = user.circles.map(formatCircle);
    res.json(circles);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 通过邀请码加入圈子（封禁用户禁止加入）
router.post('/:id/join', auth, checkBanned, async (req, res) => {
  try {
    const { inviteCode } = req.body;

    const circle = await Circle.findOne({ inviteCode });
    if (!circle) {
      return res.status(404).json({ message: '邀请码无效' });
    }

    const isMember = circle.members.some(m => m.user.toString() === req.userId);
    if (isMember) {
      return res.status(400).json({ message: '已在该圈子中' });
    }

    if (circle.members.length >= circle.maxMembers) {
      return res.status(400).json({ message: '圈子人数已满' });
    }

    circle.members.push({ user: req.userId, role: 'member' });
    await circle.save();

    await User.findByIdAndUpdate(req.userId, {
      $push: { circles: circle._id }
    });

    res.json(formatCircle(circle));
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 格式化圈子数据，匹配前端模型
function formatCircle(circle) {
  return {
    id: circle._id.toString(),
    name: circle.name,
    description: circle.description,
    ownerId: circle.owner.toString(),
    inviteCode: circle.inviteCode,
    members: circle.members.map(m => m.user.toString()),
    maxMembers: circle.maxMembers,
    createdAt: circle.createdAt?.getTime() || Date.now()
  };
}

module.exports = router;
