const express = require('express');
const cors = require('cors');
require('dotenv').config();

const connectDB = require('./config/db');
const authRoutes = require('./routes/auth');
const goodItemRoutes = require('./routes/goodItems');
const uploadRoutes = require('./routes/upload');

const app = express();
const PORT = process.env.PORT || 3002;

// 连接数据库
connectDB();

// 中间件
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 路由
app.use('/api/auth', authRoutes);
app.use('/api/good-items', goodItemRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/uploads', express.static('uploads'));

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'goodx-server', version: '0.1.0' });
});

// 错误处理
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ message: '服务器内部错误' });
});

app.listen(PORT, () => {
  console.log(`GoodX Server running on port ${PORT}`);
});
