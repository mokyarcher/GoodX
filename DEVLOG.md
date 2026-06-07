# GoodX - 开发日志

## 格式规范
每条记录包含：
- 日期
- 工作内容
- 技术决策/问题/解决方案

---

## 2026-06-07

### 今日工作
- 项目立项，确定 GoodX 定位：私有圈子好物分享平台
- 建立文档体系（AGENTS.md / PLAN.md / CHANGELOG.md / DEVLOG.md）
- 规划 MVP 功能范围：用户系统 + 圈子 + 好物发布浏览

### 技术决策
- **Android**：沿用现有项目的 Kotlin + Compose 技术栈，保持一致性
- **后端**：Node.js + Express，与 download-site 统一技术栈
- **数据库**：MongoDB，内容型应用 Schema 灵活
- **部署**：复用现有服务器和 PM2 进程管理

### 待解决问题
- [ ] 后端 API 详细设计
- [ ] MongoDB Schema 设计
- [ ] Android 项目模块划分
- [ ] 用户认证方案（JWT vs Session）

### 下一步
1. 搭建 Android Studio 项目骨架
2. 设计数据库 Schema
3. 实现用户注册/登录 API

---

## 2026-06-07（续）

### 今日工作
- ✅ 搭建 Android 项目骨架
- 创建包结构：`team.sharex.goodx`
- 配置 Gradle 构建（libs.versions.toml）
- 添加依赖：Compose、Navigation、Retrofit、Coil、Room、DataStore
- 创建数据模型：User、Circle、GoodItem
- 创建 APEX 风格主题（无圆角、红黑配色）
- 配置 AndroidManifest.xml

### 项目结构
```
app/src/main/java/team/sharex/goodx/
├── GoodXApplication.kt      # Application 入口
├── MainActivity.kt           # 主 Activity
├── model/
│   ├── User.kt              # 用户模型
│   ├── Circle.kt            # 圈子模型
│   └── GoodItem.kt          # 好物模型（8个分类）
├── ui/
│   ├── theme/
│   │   ├── Color.kt         # APEX 风格配色
│   │   ├── Theme.kt         # Dark Theme
│   │   └── Type.kt          # 字体规范
│   ├── screens/             # 页面（待实现）
│   ├── components/          # 组件（待实现）
│   └── navigation/          # 导航（待实现）
├── data/
│   ├── local/               # Room / DataStore
│   ├── remote/              # Retrofit API
│   └── repository/          # 仓库层
└── viewmodel/               # ViewModel（待实现）
```

### 技术决策
- **分类体系**：8 大分类（电子/生活/服饰/游戏/影视/阅读/软件/订阅）
- **主题风格**：延续 download-site 的 APEX 红黑风格，无圆角
- **架构模式**：MVVM + Repository + Compose Navigation

### 遇到的问题
- 本地环境 Java 8 无法运行 Gradle 9.4.1（需要 Java 17+）
- **解决方案**：在 Android Studio 中打开项目，使用内置 JDK 构建

---

## 2026-06-07（续）

### 今日工作
- ✅ 搭建 Node.js 后端骨架
- ✅ 设计 MongoDB 数据库 Schema
- ✅ 实现用户认证 API（注册/登录）
- ✅ 实现圈子 API（创建/加入/列表）
- ✅ 实现好物 API（发布/列表/详情/点赞）

### 后端项目结构
```
server/
├── app.js                   # Express 入口
├── config/
│   └── db.js               # MongoDB 连接
├── middleware/
│   └── auth.js             # JWT 认证中间件
├── models/
│   ├── User.js             # 用户模型
│   ├── Circle.js           # 圈子模型
│   └── GoodItem.js         # 好物模型
└── routes/
    ├── auth.js             # 注册/登录
    ├── circles.js          # 圈子管理
    └── goodItems.js        # 好物 CRUD
```

### API 清单

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |
| POST | /api/circles | 创建圈子 | 是 |
| GET | /api/circles/my | 我的圈子 | 是 |
| POST | /api/circles/join | 加入圈子 | 是 |
| POST | /api/good-items | 发布好物 | 是 |
| GET | /api/good-items/circle/:id | 圈子好物列表 | 是 |
| GET | /api/good-items/:id | 好物详情 | 是 |
| POST | /api/good-items/:id/like | 点赞/取消点赞 | 是 |

### 技术决策
- **认证方案**：JWT Token（7天有效期），通过 Authorization: Bearer 头部传递
- **密码加密**：bcryptjs（10轮 salt）
- **邀请码**：UUID 前8位大写
- **权限控制**：中间件检查 JWT + 圈子成员身份验证

### 遇到的问题
- 本地无法测试后端（没有 MongoDB 环境）
- **解决方案**：后续在服务器上部署测试

### 下一步
1. 在服务器部署后端服务
2. 实现 Android 端的 Retrofit API 接口
3. 创建登录/注册页面 UI
4. 创建圈子列表和创建页面
