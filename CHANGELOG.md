# GoodX 开发记录

记录 GoodX 从开发、调试到优化过程中的重要改动、思路和方法。不是严格发布日志，重点是方便快速理解项目演进。

---

## 2026-06-22：接入阿里云图片内容审核

### 内容

- 发帖时自动审核图片，调用阿里云内容安全（绿网）图片审核增强版
- 高风险/中风险图片拦截，返回"图片包含违规内容，禁止发布"
- 审核服务异常时默认放行，避免影响正常发帖

### 方法

- 服务端新增 `server/utils/imageModeration.js`，封装阿里云 SDK 调用
- 使用 `Green.ImageModerationRequest` + `imageModerationWithOptions` 同步接口
- 服务代码 `baselineCheck`，参数 `imageUrl`（公网可访问完整 URL）
- App 上传返回相对路径 `/uploads/xxx.jpg`，审核前自动拼接为 `http://111.229.166.216:3002/uploads/xxx.jpg`

### 踩坑

- pm2 下 `process.env` 不继承 shell 环境变量，`dotenv` 也可能失效
- 最终方案：在 `imageModeration.js` 里直接用 `fs.readFileSync` 读取 `.env` 文件解析 AK/SK
- SDK 版本 `@alicloud/green20220302@3.3.0`，请求对象从 `Green` 模块直接导出，不是 `Green.default`

---

## 2026-06-22：违禁词库清理与部署

### 内容

- 从电商/广告法词库导入的违禁词库中，清理掉 38 个过度宽泛的极限词（如单字"最"）
- 词库从 3490 条缩减到 2753 条，避免正常评论（如"最大"、"最好"）被误拦截
- 词库文件独立为 `server/config/sensitive-words.json`，服务端启动时加载

### 说明

- 部署路径：`/opt/goodx/config/sensitive-words.json`（不是 `server/config/`）
- 词库管理流程：用户编辑 JSON → 通知助手 → scp 上传 → `pm2 restart goodx-api`

---

## 2026-06-19：发布 v0.7.7（versionCode 61）

### 内容

- 发现页支持下拉刷新：触顶继续下拉触发刷新，列表内容随手势下移并缓慢回弹
- 优化发现页滚动帧率：下拉刷新改用 graphicsLayer，缓存卡片复杂 modifier
- 帖子详情评论区交互优化：点击评论回复直接聚焦底部输入栏，无需再点输入框
- 帖子详情评论区视觉调整：评论卡片保留液态玻璃，发送/取消按钮改为普通圆角按钮

### 说明

- 发布 APK 文件名：`goodx-v61.apk`
- 线上版本：0.7.7 / versionCode 61
- 注意清理 Gradle 缓存并使用唯一文件名，避免系统安装器缓存旧包

---

## 2026-06-19：发布 v0.7.6（versionCode 60）

### 内容

- 帖子详情页评论支持楼中楼（点击评论回复，回复显示在对应楼层下）
- 评论区改用磨砂玻璃质感卡片，主评论一卡一楼，楼中楼共卡
- 统一点赞图标：帖子互动区、发现页卡片、评论点赞均使用 Material 爱心图标
- 帖子互动区改为「点赞 / 收藏 / 转发」一行布局
- 删除帖子详情页底部缩略图选择栏，改为滑动/点击主图查看
- 优化底部评论输入框高度，与发送按钮对齐，提示文字垂直居中

### 说明

- 服务端同步支持 `parentId` 字段存储回复关系
- 下载站同步支持带版本号的 APK 文件名，避免系统安装器缓存旧包
- 线上版本：0.7.6 / versionCode 60

---

## 2026-06-07：项目初始化

### 内容

- 创建 GoodX Android 项目
- 确定包名：`team.sharex.goodx`
- 搭建 Kotlin + Jetpack Compose + Material3 基础结构
- 添加 Navigation、Retrofit、OkHttp、Coil、Room、DataStore 等依赖
- 创建基础模型：User、Circle、GoodItem
- 搭建 Node.js + Express 后端骨架
- 配置 MongoDB、JWT、bcryptjs

### 说明

早期方案包含“圈子 / 邀请制”概念，后续已废弃。

---

## 2026-06-07：后端基础 API

### 内容

- 实现注册 / 登录
- 实现 JWT 认证中间件
- 实现好物发布、列表、详情、点赞
- 实现图片上传基础能力

### 方法

- Express 路由分层
- MongoDB 存储用户和好物
- 图片本地保存到 uploads 目录

---

## 2026-06-08：UI 方向调整

### 内容

- 从早期 APEX 红黑风调整为浅青白液态玻璃风格
- 发现页卡片改为图片 + 玻璃信息区结构
- 底部导航改成轻量风格
- 主题色转向 Tiffany 青：`#0ABAB5`

### 方法

- Compose 自定义 `LiquidGlassCard`
- 背景光场 + 半透明渐变 + 高光线条

---

## 2026-06-09：液态玻璃增强

### 内容

- 重做 `GlassCard.kt`
- 增加页面级液态玻璃背景 `LiquidGlassBackdrop`
- 卡片增加边缘高光、内阴影和斜向光带
- 图片与右侧玻璃区之间增加渐变融合

### 问题修复

- 修复 `Offset.Infinite` 导致的绘制崩溃问题
- 点赞触发重绘时不再闪退

### 经验

Compose 渐变不要使用无限坐标，绘制阶段可能崩溃。使用有限坐标更安全。

---

## 2026-06-09：点赞和脏数据容错

### 内容

- GoodItem / Author / Comment 模型增强空安全
- UI 使用 `orEmpty()` 和空安全访问
- 点赞接口返回前重新 populate author 和 comments.user

### 目的

避免数据库历史脏数据或接口字段缺失导致 App 崩溃。

---

## 2026-06-09：列表图片缩略图优化

### 内容

- 新增服务端缩略图接口：`/api/upload/thumb/:filename`
- 使用 Sharp 生成 WebP 缩略图
- 发现页、我的发布列表优先加载缩略图
- 缩略图接口失败时自动回退原图

### 方法

- 服务器缓存到 `uploads/thumbs/`
- 历史图片按需生成，不需要迁移数据库

---

## 2026-06-09：服务端缩略图部署

### 内容

- 部署到 `/opt/goodx`
- 安装 `sharp`
- 重启 PM2 进程 `goodx-api`
- 验证 `/health` 和缩略图接口

### 结果

线上缩略图接口可用，返回 WebP，并带长期缓存头。

---

## 2026-06-09：Logo 设计和 App 图标实装

### 内容

- 基于用户草图生成 GoodX Logo SVG
- 最终采用 `X + GOOD` 液态玻璃版本
- 修复 Adaptive Icon PNG 尺寸错误导致的图标发糊问题

### 方法

- Android Adaptive Icon foreground 使用 108dp 系列尺寸：
  - mdpi 108
  - hdpi 162
  - xhdpi 216
  - xxhdpi 324
  - xxxhdpi 432

### 经验

Adaptive Icon 的 foreground 不能按传统 48/72/96 尺寸导出，否则系统放大会糊。

---

## 2026-06-09：详情页图片加载体验优化

### 内容

- 详情页默认不再直接加载原图
- 主图和缩略图优先加载压缩图
- 点击主图进入全屏查看器
- 全屏默认显示压缩图
- 用户点击“查看原图”后才加载原图
- 已加载过原图的图片再次查看时直接展示原图
- 全屏底部控件调整为：左下载、中查看原图、右页码
- 加载原图后隐藏“查看原图”按钮

### 目的

提升详情页打开速度，减少流量浪费，同时保留查看高清原图的能力。

---

## 2026-06-10：详情页预览图质量优化

### 内容

- 新增服务端预览图接口：`/api/upload/preview/:filename`
- 上传图片时同时生成缩略图和预览图，历史图片仍按需生成
- 列表和详情页缩略横条继续使用 thumb
- 详情页主图和全屏默认图改用 preview
- 用户点击“查看原图”后才加载 original

### 方法

- thumb：360x360 WebP quality 76，列表使用
- preview：长边 1280 WebP quality 86，详情页和全屏默认使用
- original：保留 `/uploads/:filename`，用于用户主动查看/下载

### 验证

- `node --check server/routes/upload.js` 通过
- `:app:compileDebugKotlin` 通过

---

## 2026-06-10：全屏图片查看器交互完善

### 内容

- 修复进入全屏时压缩图不显示、只出现黑屏的问题
- 全屏图片采用多层兜底：thumb 兜底、preview 覆盖、original 用户主动触发
- 修复点击“查看原图”时原图和压缩图叠加错位的问题
- 原图加载中显示居中加载圆圈，保留压缩图兜底
- 原图加载成功后隐藏压缩图层，只显示原图
- 同次 App 运行内记录已成功查看过原图的图片，再次进入详情/全屏时不再显示“查看原图”按钮，直接按原图状态展示
- 原图查看状态从单个详情页内部状态改为文件级运行期缓存，按图片路径记录
- 全屏底部下载 / 查看原图 / 页码按钮缩小到约原来的 2/3
- 原图支持双指缩放和拖拽查看细节，最大 5x，缩回 1x 时自动居中

### 下载保存优化

- 原图下载从系统 `DownloadManager` 改为 App 内部静默下载
- Android 10+ 使用 `MediaStore` 保存到系统相册
- 低版本写入 `Pictures` 并触发媒体扫描
- 去掉系统“下载管理服务”通知卡片
- App 内 Toast 提示：开始下载原图 / 成功保存到相册 / 下载失败

### 经验

- 全屏图片不要在请求原图时直接替换当前图片 URL，否则网络加载期间会黑屏
- 原图加载成功后必须隐藏压缩图层，否则不同图片处理链路可能导致叠加错位
- `remember(imagePath, showOriginal)` 会在 showOriginal 变化时重置加载成功状态，原图 ready 状态应只跟随当前图片路径重置
- 系统 `DownloadManager` 会显示系统下载通知，视觉上偏工具感；面向用户的保存到相册体验更适合 App 内下载 + MediaStore

### 验证

- 多次 `:app:compileDebugKotlin` 通过
- 多次 `:app:assembleDebug` 生成实体机测试 APK

---

## 2026-06-10：三大内容类型规划

### 背景

GoodX 原先以 8 个基础品类承载所有内容，随着定位扩展，需要同时覆盖：

- 物品/商品/工具类推荐
- 风景、动态、地点、生活瞬间
- 电影、音乐、书籍、游戏等文化娱乐内容

用户确定新的一级内容类型为：

```text
好物 / 此刻 / 文娱
```

### 功能优化计划

#### 1. 数据结构

- 新增一级类型字段：`contentType`
  - `GOODS`：好物
  - `MOMENTS`：此刻
  - `ENTERTAINMENT`：文娱
- 保留现有 `category` 作为细分品类字段，避免直接破坏旧数据
- 保留 `subCategory` 作为补充标签/自定义词条，后续可逐步拆成更明确字段
- 旧数据迁移策略：没有 `contentType` 的内容默认按现有 `category` 映射到对应一级类型

#### 2. 发布页交互

- `+ 发布` 页面顶部新增三段式切换：好物 / 此刻 / 文娱
- 用户选择不同一级类型后，动态切换：
  - 标题占位文案
  - 描述占位文案
  - 细分品类下拉项
  - 可选补充词条名称

建议文案：

| 类型 | 标题占位 | 描述占位 | 补充词条 |
|------|----------|----------|----------|
| 好物 | 这个好物叫什么？ | 为什么推荐它？使用体验如何？ | 品牌 / 平台 / 场景 |
| 此刻 | 这一刻是什么？ | 记录一下当时看到/感受到的东西 | 地点 / 场景 |
| 文娱 | 这部作品叫什么？ | 为什么值得看/听/读/玩？ | 作者 / 平台 / 状态 |

#### 3. 细分品类

- 好物：电子数码、生活日用、服饰穿搭、软件工具、订阅服务、其他好物
- 此刻：风景、城市、旅行、日常、地点、其他此刻
- 文娱：电影、剧集、音乐、阅读、游戏、动漫、播客、其他文娱

#### 4. 列表和详情展示

- 发现页卡片展示一级类型 + 细分品类，例如：`此刻 · 风景`
- 详情页分类行同步展示一级类型和细分品类
- 旧内容没有 `contentType` 时，通过 `category` 映射出一级类型，避免展示空值

#### 5. 后端兼容

- GoodItem 模型新增 `contentType`，设置默认值或兼容旧数据
- 发布接口接受 `contentType/category/subCategory`
- 列表接口后续支持按 `contentType` 筛选
- 不删除旧枚举，先做兼容演进

### 注意事项

- 规划阶段先记录方向，实现时优先保证旧数据可读、旧接口不崩
- UI 风格保持轻量液态玻璃，不做复杂重表单

---

## 2026-06-10：三大内容类型第一阶段实现

### 内容

- 后端 GoodItem 模型新增 `contentType`
- 后端发布 / 修改 / 列表接口兼容 `contentType`
- 后端格式化返回 `contentType`，旧数据按 `category` 推断一级类型
- Android `GoodItem` 新增 `ContentType`
- Android `Category` 扩展为三大类下的细分品类
- 发布页新增 `好物 / 此刻 / 文娱` 三段式切换
- 发布页根据一级类型动态切换：标题占位、正文占位、补充词条、细分品类下拉项
- 发现页和详情页分类展示改为：`大类 · 细分品类`

### 兼容策略

- `contentType` 默认值为 `GOODS`，避免旧数据和旧调用崩溃
- 后端未收到合法 `contentType` 时，按 `category` 自动映射到好物 / 此刻 / 文娱
- 旧字段 `category/subCategory` 保留，暂不做破坏性迁移

### 验证

- `node --check server/models/GoodItem.js` 通过
- `node --check server/routes/goodItems.js` 通过
- `:app:compileDebugKotlin` 通过

---

## 2026-06-11：全部页两级分类结构

### 内容

- `全部` Tab 不再平铺所有细分品类
- `全部` Tab 默认只展示三大类：好物 / 此刻 / 文娱
- 新增大类详情页：点击大类后展示该大类下的细分品类
- 点击细分品类后进入原有分类信息流
- 分类信息流标题改为：`大类 · 细分品类`
- 给 `ContentType` 增加图标和副标题展示信息
- 给 `Category` 增加描述文案，用于细分品类卡片

### 交互路径

```text
全部
  → 好物 / 此刻 / 文娱
  → 对应细分品类
  → 分类信息流
```

### 目的

减少全部页信息密度，避免所有子类挤在一个页面里，让三大内容类型成为清晰入口。

### 验证

- `:app:compileDebugKotlin` 通过

---

## 2026-06-12：近期 UI 与图片体验小优化

### 内容

- 全部页 UI 精简：去掉顶部说明文案“先选择大类，再查看细分内容”
- 全部页大类卡片精简：去掉“x 个分类”，只保留大类名和描述
- 详情页顶部标题下移，避免“好物详情”和系统状态栏/打孔区域距离过近
- 全屏原图加载状态优化：原图层改用 `AsyncImage` 的 `onSuccess` 直接关闭加载圆圈
- 原图层关闭 crossfade，减少图片已显示但加载圆圈仍短暂停留的问题

### 验证

- `:app:compileDebugKotlin` 通过
- `:app:assembleDebug` 通过

---

## 2026-06-13：个人中心与管理员后台大更新

### 内容

#### 个人资料与头像

- 头像上传与展示
- 昵称修改（用户名不可改，仅用于登录）
- 编辑资料全屏页面
- 个人页展示头像 + 昵称 + @用户名
- 修改密码功能

#### 消息中心

- 通知系统（Notification 模型）
- 帖子被下架自动通知作者
- 提交审核通知管理员
- 审核通过/拒绝通知作者
- 未读红点 + 全部已读
- 通知跳转已下架/管理员后台

#### 管理员后台

- admin 账户自动创建（Mqm112358）
- 管理员可见用户列表
- 封禁/解封用户
- 删除用户（不可删除管理员）
- 查看用户所有帖子
- 下架帖子（需填写下架理由）
- 审核通过/拒绝重新上架
- 完整审核流程

#### 在线更新

- 版本检查接口 `/api/version`
- APK 静态托管 `/apk/goodx.apk`
- 检查更新 → 下载进度条 → 自动安装
- HttpURLConnection 直连下载（避开 DownloadManager 兼容问题）
- ACTION_VIEW 安装（兼容 ColorOS Android 16）
- 唯一文件名（避免 FileProvider URI 缓存）
- 双重返回退出防误触
- Gradle 配置缓存问题发现与修复

#### 卡片与交互

- GoodItemCard 液态玻璃完整重写
- 卡片显示作者头像 + 昵称
- 点赞数 + 评论数 + 最新互动（xxx 刚刚评论/点赞）
- 详情页评论点赞
- 全屏图片 HorizontalPager 左右滑动切图
- 详情页滚动位置记忆（saveable state）
- 底部导航支持滑动切标签
- 发现页 ↻ 下拉刷新

#### UI 优化

- 退出登录确认弹窗
- 关于 GoodX 弹窗
- 输入框统一样式（浅色框线）
- 评论框与发送按钮高度统一
- 发布页去平台显示
- 卡片去 subCategory 显示
- 详情页去顶栏标题

### 踩坑记录

- Gradle Configuration Cache 导致 versionCode 不更新，需 `--no-configuration-cache`
- DownloadManager 在不同 Android 版本上行为不一致
- FileProvider + 同文件名导致第二次安装读旧包
- ColorOS Android 16 不支持 ACTION_INSTALL_PACKAGE，改用 ACTION_VIEW
- HomeScreen.kt 被 git checkout 覆盖后完整重建
- GoodX 独立仓库，不与父仓库混用

### 验证

- `:app:compileDebugKotlin` 通过数十轮
- `:app:assembleDebug` 多次线上部署验证
- OPPO Android 16 + 联想平板 Android 15 兼容通过

---

## 2026-06-14：0.6.x / 0.7.x 体验修复与恢复记录

### 背景

一次误操作将 `HomeScreen.kt` 回退到早期状态，随后完整重建主要 UI 功能。此后必须严格遵守：改动前确认仓库、改动后立即 commit，禁止无 commit 的破坏性恢复命令。

### 内容

#### 首页与导航

- 重建 `HomeScreen.kt`：恢复发现页、全部页、圈子页、我的页
- 底部导航恢复为：发现 / 全部 / + / 圈子 / 我的
- 底部导航支持左右滑动切换，并修复点击 tab 时滑动中间页导致卡住的问题
- 发现页标题高度、字号与粗细多轮调整
- 发现页刷新最终采用稳定的 `↻` 按钮方案；下拉刷新方案暂缓，因为 nestedScroll 与 LazyColumn 组合体验不稳定

#### 全部页

- 取消“大类 → 细分类 → 信息流”的复杂路径
- 改为“全部页直接信息流 + 顶部大类筛选”
- 默认选中“好物”
- 好物 / 此刻 / 文娱 筛选状态使用 `rememberSaveable` 保留，切换底部导航后不丢失
- 筛选按钮点击波纹裁剪为圆角

#### 发布 / 编辑限制

- 发布页和编辑页标题限制 10 字
- 正文限制 500 字
- 图片最多 20 张
- 分批选图改为追加，不再覆盖上一批
- 超过 20 张自动截取并提示

#### 图片查看

- 详情页主图支持左右滑动切图
- 全屏原图查看支持左右滑动切图
- 原图层手势冲突修复：未缩放时允许 pager 滑动，放大后才处理拖拽缩放

#### 管理员后台

- 新增“全部帖子管理”，可处理已删除用户遗留帖子
- 后台管理页按原型改为：顶部主 Tab（用户 / 帖子）
- 用户 Tab 子筛选：全部 / 封禁 / 注销（注销先占位）
- 帖子 Tab 子筛选：所有 / 匿名用户 / 已下架

#### 评论与互动

- 评论区新增评论点赞
- 评论未点赞状态视觉优化，空心心形更淡、更紧凑

#### 冷启动与缓存

- 新增 SplashScreen
- Splash 阶段预拉发现页数据、用户信息、未读数，并预热首屏缩略图
- RetrofitClient 增加 20MB OkHttp 磁盘缓存
- 发现页优先读内存缓存，减少冷启动后首屏卡顿

#### 在线更新机制

- 最终稳定方案：HttpURLConnection 直连下载 + FileProvider + ACTION_VIEW 安装
- 禁用 HttpURLConnection 缓存：`useCaches = false` + `Cache-Control: no-cache, no-store`
- 每次下载使用唯一文件名，避免 FileProvider / 系统安装器 URI 缓存旧包
- 发现 Gradle Configuration Cache 会导致 versionCode 未更新，发布时必须禁用配置缓存并 clean build

### 发布注意事项

发布 APK 必须使用：

```bash
rm -rf .gradle/configuration-cache
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
./gradlew :app:clean :app:assembleDebug --no-configuration-cache
```

否则可能出现线上版本接口已更新，但 APK 实际 versionCode 未变的问题。

### 验证

- 多轮 `:app:compileDebugKotlin --no-configuration-cache` 通过
- 多轮 `:app:assembleDebug --no-configuration-cache` 通过
- OPPO Android 16 和联想平板 Android 15 均已验证在线更新可连续成功

---

## 2026-06-17：GoodX v0.7.3 发布

### 内容

- 版本号升级到 `0.7.3`（versionCode 57）
- 配置 Coil 共享 ImageLoader，加大磁盘/内存缓存与网络超时
- 全屏图片查看器不再默认加载 20MB 原图，改为点击"原图"按钮才加载
- 详情页图片加载失败时先重试一次，再回退原图
- 服务端上传接口改为后台异步生成缩略图，避免上传栏卡灰
- 重新打包 APK 并部署到下载站

### 验证

- `:app:clean :app:assembleDebug --no-configuration-cache` 通过
- 服务器 `/api/version` 返回 `0.7.3` / `versionCode: 57`
- APK 已部署到 `/opt/projects/download-site/public/download/goodx.apk`
- 下载站首页已更新 GoodX 版本信息

---

## 2026-06-17：GoodX v0.7.4 发布

### 内容

- 版本号升级到 `0.7.4`（versionCode 58）
- 优化页面过渡动画：去掉淡入淡出与纵向偏移，改为纯横向左右滑动
- 全屏图片查看器去掉"原图"按钮，恢复打开后自动加载原图
- 重新打包 APK 并部署到下载站

### 验证

- `:app:clean :app:assembleDebug --no-configuration-cache` 通过
- 服务器 `/api/version` 返回 `0.7.4` / `versionCode: 58`
- APK 已部署，下载站版本信息已更新

---

## 2026-06-15：切换新服务器地址

### 背景

旧服务器 `124.223.50.79:3002` 即将过期，GoodX 后端测试/线上地址切换到新服务器 `111.229.166.216:3002`。

### 内容

- Android API `BASE_URL` 切换到新服务器
- 修复图片加载仍写死旧服务器 IP 的问题，图片地址统一跟随 `RetrofitClient.BASE_URL`
- 服务端 `/api/version` 的 `apkUrl` 切换到新服务器地址
- `agent.md` 当前技术事实同步更新为新服务器

### 验证

- 新服务器 `/health`、`/api/version`、`/api/good-items` 冒烟测试通过
- 新服务器 `/uploads`、`/api/upload/thumb`、`/api/upload/preview` 图片接口测试通过
- `:app:compileDebugKotlin --no-configuration-cache` 通过

---

## 2026-06-18：发现页分页加载

### 背景

发现页原先一次性拉取全部帖子，数据量增长后首屏加载慢、浪费流量。用户要求发现页与「全部页」做业务区隔：发现页是「最新流」，首屏只拉最新 30 条，下滑再逐步加载。

### 内容

- `HomeScreen.kt` 的 `DiscoverTab` 改为分页加载：
  - 首屏 `page=1, limit=30`
  - 触底后自动加载下一页 `page++, limit=20`
  - 使用 `rememberLazyListState` + `derivedStateOf` 检测接近底部（剩余 3 项时触发）
  - 新增底部 footer：加载中 / 加载失败重试 / 没有更多了
  - 保留标题栏 `↻` 刷新按钮，点击后重置到第一页
  - 初始加载失败时显示全屏错误 + 重试按钮
- `SplashScreen.kt` 预加载 `limit` 从 20 改为 30，与发现页首屏保持一致，减少重复请求
- 服务端无需改动，复用已有 `page`/`limit` 参数

### 注意事项

- 未引入 Paging 3，保持项目依赖轻量
- 未改动「全部页」`AllCategoriesTab` 的筛选逻辑，两个 Tab 业务独立
- 未做下拉刷新，继续沿用稳定的标题栏 `↻` 按钮方案

### 验证

- `:app:compileDebugKotlin --no-configuration-cache` 通过
- Git commit `969aa07` 已提交到 GoodX 独立仓库

### 发布

- 版本号升级到 `0.7.5`（versionCode 59）
- 重新打包 APK 并部署到下载站
- `/api/version` 返回 `0.7.5` / `versionCode: 59`
- APK 已部署到 `/opt/projects/download-site/public/download/goodx/goodx.apk`（13.9M）
- 下载站首页已更新：`https://www.sharex.team/download/goodx/`
- Git commit `720210b` 已提交到 GoodX 独立仓库

### 问题修复

- 首次部署后用户更新仍提示「已安装相同版本 0.7.4」
- 原因：虽然 `build.gradle.kts` 已改为 versionCode 59，但首次打包时 Configuration Cache 仍导致 APK 内部版本为 58；同时 `goodx.apk` 同文件名也容易被系统安装器缓存
- 解决：
  - 彻底删除 `.gradle` / `app/build` / `build` 后重新打包
  - 验证 APK 内部 `versionCode=59`、`versionName=0.7.5`
  - `apkUrl` 改为唯一文件名 `/apk/goodx/goodx-v59.apk?v=59`
  - 服务器同时保留 `goodx.apk` 和 `goodx-v59.apk`

### 验证

- 重新打包后 APK 内部版本确认：59 / 0.7.5
- 服务器 `/api/version` 返回 `0.7.5` / `versionCode: 59` / `apkUrl` 指向 `goodx-v59.apk`
- 下载站已重新生成：`https://www.sharex.team/download/goodx/`
- Git commit `c84dbce` 已提交到 GoodX 独立仓库

---

## 2026-06-18：发布/编辑网络错误提示优化

### 内容

- `CreateGoodItemScreen.kt` 发布时捕获网络异常不再显示 `e.message`（避免暴露服务器 IP）
- 统一显示为「网络错误，点击重试」，点击错误文案可直接重试发布
- `EditGoodItemScreen.kt` 保存时同步处理，避免同样问题

### 验证

- `:app:compileDebugKotlin --no-configuration-cache` 通过
- Git commit `1a5c49b` 已提交到 GoodX 独立仓库

---

## 2026-06-18：文档整理，产品事实归位到 design.md

### 内容

- 将 `agent.md` 中属于产品/技术事实的内容迁移到 `design.md`：
  - 内容方向（好物 / 此刻 / 文娱）
  - 发布页分类策略与补充字段
  - 交互经验（发现页、全部页、后台管理、发布/编辑限制）
  - 图片加载策略细节
  - 当前运行环境（版本、服务器、缓存等）
- `agent.md` 精简为 AI / 开发助手的工作规则、常用命令、发布检查清单

### 验证

- Git commit `c9e36cc` 已提交到 GoodX 独立仓库
