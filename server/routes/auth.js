const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const User = require('../models/User');
const auth = require('../middleware/auth');

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'goodx-secret-key';

// 注册
router.post('/register', async (req, res) => {
  try {
    const { username, password, nickname } = req.body;

    const existingUser = await User.findOne({ username });
    if (existingUser) {
      return res.status(400).json({ message: '用户名已存在' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const inviteCode = uuidv4().substring(0, 8).toUpperCase();

    const user = new User({
      username,
      password: hashedPassword,
      nickname: nickname || username,
      inviteCode
    });

    await user.save();

    const token = jwt.sign({ userId: user._id }, JWT_SECRET, { expiresIn: '7d' });

    res.status(201).json({
      token,
      user: {
        id: user._id.toString(),
        username: user.username,
        nickname: user.nickname,
        avatar: user.avatar,
        inviteCode: user.inviteCode,
        createdAt: user.createdAt?.getTime() || Date.now()
      }
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 登录
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    const user = await User.findOne({ username });
    if (!user) {
      return res.status(400).json({ message: '用户名或密码错误' });
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(400).json({ message: '用户名或密码错误' });
    }

    const token = jwt.sign({ userId: user._id }, JWT_SECRET, { expiresIn: '7d' });

    res.json({
      token,
      user: {
        id: user._id.toString(),
        username: user.username,
        nickname: user.nickname,
        avatar: user.avatar,
        inviteCode: user.inviteCode,
        createdAt: user.createdAt?.getTime() || Date.now()
      }
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

// 获取当前用户
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.userId);
    if (!user) {
      return res.status(404).json({ message: '用户不存在' });
    }
    res.json({
      id: user._id.toString(),
      username: user.username,
      nickname: user.nickname,
      avatar: user.avatar,
      inviteCode: user.inviteCode,
      createdAt: user.createdAt?.getTime() || Date.now()
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
});

module.exports = router;
