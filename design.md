# GoodX 设计文档

## 产品定位

面向用户的全品类好物分享平台。用户分享生活中的优质物品/内容，涵盖实物（电子产品、生活用品）和虚拟（游戏、电影、小说、软件等）。

**定位区别：不是社交平台，而是专注“值得分享的东西”。**

---

## 核心特点

- **全品类覆盖**：实物 + 虚拟，生活 + 娱乐 + 生产力工具
- **视觉优先**：液态玻璃 UI，轻量、柔和、高级
- **流畅优先**：缩略图 + 原图分级加载，不浪费流量
- **Android 原生**：Compose 驱动，轻量 App

---

## 已落地功能

### 认证

- 手机号/用户名注册
- 登录 / JWT Token 7 天有效期
- 自动登录

### 内容核心

- 内容发布：标题 + 描述 + 图片 + 大类 + 细分品类 + 可选补充字段
- 内容详情：大图 + 描述 + 发布者 + 时间
- 三大内容类型：好物 / 此刻 / 文娱
- 细分品类：随大类动态切换
- 多图支持
- 点赞 / 取消点赞
- 评论系统（含评论点赞）
- 发现页时间线列表（当前采用稳定的 ↻ 按钮刷新）
- 全部页：大类筛选信息流（好物默认选中，记住上次筛选）
- 我的发布列表（含已发布/已下架标签切换）
- 下架-整改-提交审核-管理员审批流程
- 帖子标题 10 字限制、正文 500 字限制、图片最多 20 张

### UI / 体验

- 液态玻璃卡片（含作者头像昵称、互动统计）
- 青白色主题 + Tiffany 青强调色
- 底部导航（发现/全部/+/圈子/我的）+ 滑动切标签
- 分类目录：全部页直接显示帖子流，顶部大类筛选（好物 / 此刻 / 文娱）
- 详情页预览图优先，全屏自动加载原图
- 全屏图片查看器：左右滑动切图、压缩图兜底、原图缩放/拖拽、原图预加载
- 原图静默保存到相册
- 个人资料页：头像 + 昵称编辑 + 修改密码
- 管理员后台：用户/帖子双 Tab，用户筛选（全部/封禁/注销占位），帖子筛选（所有/匿名/已下架）
- 管理员可管理已删除用户遗留帖子
- 消息中心：系统通知、未读标记、一键跳转已下架/后台审核
- 在线更新：版本检查、下载进度、自动安装
- 退出登录确认 + 关于弹窗

### 后端

- Node.js + Express
- MongoDB
- 图片本地上传 + Sharp 缩略图生成
- PM2 部署
- 管理员 API：用户管理、帖子审核
- 通知系统：自动推送下架/审核消息

---

## 分类体系

GoodX 后续采用三大内容类型作为一级入口：

| 类型 | 英文 | 定位 | 适合内容 |
|------|------|------|------|
| 好物 | GOODS | 值得入手、值得使用、值得推荐的东西 | 实物商品、数码、生活用品、软件工具、订阅服务 |
| 此刻 | MOMENTS | 值得记录、值得看见、值得分享的瞬间 | 风景、旅行、城市角落、日常动态、地点体验 |
| 文娱 | ENTERTAINMENT | 值得观看、聆听、阅读、体验的文化娱乐内容 | 电影、剧集、音乐、书籍、游戏、动漫、播客 |

### 细分品类建议

#### 好物

| 英文 | 中文 | 说明 |
|------|------|------|
| ELECTRONICS | 电子数码 | 手机、电脑、耳机、智能设备 |
| LIFESTYLE | 生活日用 | 家居、厨房、户外、文具 |
| FASHION | 服饰穿搭 | 服装、鞋靴、配饰 |
| SOFTWARE | 软件工具 | App、生产力、设计、开发、效率工具 |
| SUBSCRIPTION | 订阅服务 | 流媒体、云服务、会员、数字服务 |
| OTHER_GOODS | 其他好物 | 无法归入以上类别的好物 |

#### 此刻

| 英文 | 中文 | 说明 |
|------|------|------|
| SCENERY | 风景 | 自然风景、天空、山海、季节景色 |
| CITY | 城市 | 街道、建筑、城市角落 |
| TRAVEL | 旅行 | 旅途记录、目的地、路线体验 |
| DAILY | 日常 | 生活瞬间、随手记录 |
| PLACE | 地点 | 店铺、展览、空间、公园、校园等 |
| OTHER_MOMENTS | 其他此刻 | 无法归入以上类别的瞬间 |

#### 文娱

| 英文 | 中文 | 说明 |
|------|------|------|
| MOVIE | 电影 | 电影、纪录片 |
| SERIES | 剧集 | 电视剧、网剧、综艺 |
| MUSIC | 音乐 | 歌曲、专辑、歌单、音乐人 |
| BOOK | 阅读 | 小说、非虚构、漫画、文章 |
| GAME | 游戏 | Steam、主机、手游、独立游戏 |
| ANIME | 动漫 | 动画、漫画、番剧 |
| PODCAST | 播客 | 播客、电台、音频节目 |
| OTHER_ENTERTAINMENT | 其他文娱 | 无法归入以上类别的文娱内容 |

发布页应先选择一级类型，再根据类型展示对应细分品类和补充字段。

---

## 技术栈

### Android 端

| 层级 | 技术 | 说明 |
|------|------|------|
| 语言 | Kotlin | 项目标准语言 |
| UI | Jetpack Compose + Material3 | 声明式 UI |
| 导航 | Compose Navigation | 官方导航方案 |
| 网络 | Retrofit + OkHttp | 标准 HTTP 客户端 |
| 图片 | Coil | Kotlin 友好、Compose 原生 |
| 本地存储 | Room + DataStore | 官方推荐 |
| 架构 | MVVM + Repository | 标准 App 分层 |
| 包名 | `team.sharex.goodx` | |
| 目标 SDK | 最新 | minSdk 24 |

### 后端

| 层级 | 技术 | 说明 |
|------|------|------|
| 框架 | Node.js + Express | 轻量 |
| 数据库 | MongoDB | 内容型数据，灵活 |
| 认证 | JWT Bearer Token | 7 天有效期 |
| 密码 | bcryptjs | 10 轮 salt |
| 图片处理 | Sharp | WebP 缩略图生成 |
| 存储 | 服务器本地 uploads 目录 | |
| 部署 | PM2 | 与 download-site 同服务器 |

---

## 分级图片加载体系

GoodX 采用三级图片策略，以体验为核心，不滥用流量：

| 级别 | 路径 | 用途 | 规格 |
|------|------|------|------|
| 缩略图 | `/api/upload/thumb/:file` | 发现页卡片、我的发布、详情页缩略图列表 | 360x360 cover |
| 预览图 | `/api/upload/preview/:file` | 详情页主图、全屏默认图 | 1280 长边 inside |
| 原图 | `/uploads/:file` | 用户主动点击“查看原图”后加载；已查看过则同次 App 运行内直接显示 | 原始 |

WebP quality：

- thumb: 76
- preview: 86

---

## 命名规范

### 文件 / 代码

- 屏幕：`XxxScreen.kt`
- 组件：`XxxCard / XxxHeader / XxxButton / XxxDialog`
- 数据层：`XxxApi / XxxRepository / XxxViewModel`
- 模型：`Xxx.kt`
- 主题：`Color / Theme / Type`

### API

- RESTful
- 前缀：`/api/`
- 认证路由：`/api/auth/*`
- 物品路由：`/api/good-items/*`
- 上传路由：`/api/upload/*`

### 数据库字段

- 小写下划线：`created_at, user_id`
- Mongoose timestamps：true

### 版本号

SemVer：

```text
主版本.次版本.修订号
```

例如：`1.0.0`、`0.1.0`。

---

## App 架构

```text
team.sharex.goodx/
├── GoodXApplication.kt
├── MainActivity.kt
├── model/                    # 数据模型
│   ├── GoodItem.kt
│   ├── User.kt
│   ├── Circle.kt            # 遗留字段保留，功能废弃
│   └── Comment.kt
├── ui/
│   ├── theme/               # 主题
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── screens/             # 页面
│   │   ├── HomeScreen.kt
│   │   ├── LoginScreen.kt
│   │   ├── RegisterScreen.kt
│   │   ├── CategoryDetailScreen.kt
│   │   ├── GoodItemDetailScreen.kt
│   │   ├── MyPostsScreen.kt
│   │   └── CreateGoodItemScreen.kt
│   └── components/          # 可复用组件
│       └── GlassCard.kt
├── data/
│   ├── remote/              # 网络层
│   │   ├── ApiService.kt
│   │   ├── RetrofitClient.kt
│   │   ├── TokenManager.kt
│   │   └── AuthInterceptor.kt
│   ├── local/               # Room / DataStore
│   └── repository/
└── viewmodel/
```

---

## 视觉风格

### 颜色

| 用途 | 色值 |
|------|------|
| 强调色 / Brand | `#0ABAB5` |
| 背景 | `#EFF8F7` |
| 卡片底 | 半透白渐变 |
| 主文字 | `#1C1C1E` |
| 次文字 | `#6E6E73` |
| 描边 | 白 + 青灰低透 |

### 风格关键词

- 液态玻璃
- 低对比
- 柔和高光
- 非硬边
- 低饱和青绿

### 图标

- logo 主色：`#0ABAB5`
- 背景：青白玻璃方块
- 文案：底部 GOOD

---

## 已实现的 API 端点

### 认证

```text
POST /api/auth/register
POST /api/auth/login
```

### 好物

```text
GET  /api/good-items?category=&sort=&page=&limit=
GET  /api/good-items/:id
POST /api/good-items
PUT  /api/good-items/:id
DEL  /api/good-items/:id
POST /api/good-items/:id/like
POST /api/good-items/:id/comment
```

### 图片

```text
POST /api/upload/image
GET  /api/upload/thumb/:filename
GET  /api/upload/preview/:filename
GET  /uploads/:filename
```

---

## 当前状态总结

**已可用，但未正式发布版本。**

### 已完成

- ✅ 登录 / 注册
- ✅ 发现页内容列表
- ✅ 三大类导航：好物 / 此刻 / 文娱
- ✅ 两级分类导航：大类 → 细分品类 → 信息流
- ✅ 详情页
- ✅ 发布内容：支持好物 / 此刻 / 文娱大类切换
- ✅ 我的发布
- ✅ 点赞
- ✅ 评论
- ✅ 多图上传
- ✅ 分级图片加载
- ✅ 全屏图片查看器：原图加载、保存、缩放
- ✅ 液态玻璃 UI
- ✅ 缩略图 / 预览图服务端生成
- ✅ 三大类后端兼容与线上部署
- ✅ 服务器部署上线

### 待增强

- 首页发现流按三大内容类型筛选
- 搜索
- 收藏
- 通知
- 多设备同步优化
- 本地缓存

### 已废弃概念

- **圈子**：原设计的私有圈子体系已废弃
- **邀请制**：随圈子一起废弃
- **圈子成员权限**：废弃
