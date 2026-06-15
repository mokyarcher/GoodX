# GoodX Agent 准则

本文件记录 AI / 开发助手参与 GoodX 开发时必须遵守的准则。后续可随项目推进持续更新。

---

## 项目当前方向

GoodX 当前是一个 **全品类值得分享内容 App**，一级内容类型定为：**好物 / 此刻 / 文娱**。

- 好物：实物商品、软件工具、订阅服务等值得推荐的东西
- 此刻：风景、旅行、城市角落、日常动态、地点体验等值得记录的瞬间
- 文娱：电影、剧集、音乐、书籍、游戏、动漫、播客等文化娱乐内容

**不要再基于“圈子 / 私有圈子 / 邀请制 / 圈子权限”继续设计新功能。**

这些概念属于旧方案，已废弃。

---

## 工作前必读

每次进行 GoodX 相关开发前，优先阅读：

1. `design.md`：产品设计、功能、技术栈、架构、命名规范
2. `agent.md`：当前开发准则
3. `changelog.md`：近期开发过程和重要变动

旧文档 `AGENTS.md / PLAN.md / DEVLOG.md / CHANGELOG.md` 已废弃，不再作为事实来源。

---

## 开发行为准则

### 1. 不主动打包 APK / 发布 APK

除非用户明确要求：

- “重新打包”
- “生成 APK”
- “打个包给我装手机”
- “assembleDebug / assembleRelease”

否则不要主动运行完整打包。

默认开发流程：

- 修改代码
- 必要时运行 `compileDebugKotlin` 或轻量编译检查
- 以模拟器 / Android Studio 调试为主

### 2. 修改前先确认现有实现

不要凭记忆改代码。涉及 UI、接口、模型、图片、部署时，先查对应文件。

常用入口：

```text
app/src/main/java/team/sharex/goodx/ui/screens/
app/src/main/java/team/sharex/goodx/ui/components/
app/src/main/java/team/sharex/goodx/data/remote/
app/src/main/java/team/sharex/goodx/model/
server/routes/
server/models/
```

### 3. 保持当前视觉基调

当前 UI 方向：

- 淡青白背景
- 液态玻璃卡片
- Tiffany 青强调色 `#0ABAB5`
- 轻量圆角
- 柔和高光
- 不做重黑、重红、硬边风格

除非用户明确要求，不要回到早期 APEX 红黑风。

### 4. 发布分类策略

发布页应先让用户选择一级内容类型：

```text
好物 / 此刻 / 文娱
```

不同一级类型应展示不同细分品类和补充词条：

- 好物：可强调品牌、平台、价格、购买/使用场景
- 此刻：可强调地点、时间、场景、心情/体验
- 文娱：可强调作品类型、作者/创作者、平台、观看/收听/阅读状态

实现时优先保持兼容旧数据：旧的 `category/subCategory` 不要直接废弃，应逐步映射到新结构。新增字段建议使用 `contentType` 表示一级类型，保留 `category` 表示细分品类。

### 5. UI / 交互经验

- 发现页下拉刷新暂时不要再强行做 nestedScroll 版本；当前稳定方案是标题栏 `↻` 按钮。此前 nestedScroll + LazyColumn 出现卡住圆圈、体验差的问题。
- 全部页不再使用“大卡片大类 → 细分品类”的层级，当前采用：顶部标题 + 大类筛选栏 + 帖子流。
- 后台管理按原型使用：主 Tab（用户 / 帖子）+ 子 Tab。

### 6. 图片加载策略

GoodX 图片使用分级加载：

- 列表：缩略图
- 详情：预览图
- 全屏默认：预览图，必要时用缩略图兜底，避免黑屏
- 用户主动点击：原图
- 原图加载中：保留压缩图兜底，并显示居中加载圆圈
- 原图加载成功：隐藏压缩图层，只显示原图，避免叠加错位
- 同次 App 运行内已成功查看过原图的图片，再次进入全屏时直接按原图状态处理，不再显示“查看原图”按钮

避免无脑在列表或详情首屏加载原图。图片保存不要使用系统 DownloadManager 通知，优先在 App 内静默下载并通过 MediaStore 保存到相册。

### 7. 服务端变更要考虑部署

服务端线上目录：

```text
/opt/goodx
```

PM2 进程：

```text
goodx-api
```

部署前优先备份关键文件。部署后检查：

```text
/health
pm2 status goodx-api
```

### 8. 每次改动后必须 Git Commit

**每次完成一个独立功能或修复后，立即在 GoodX 独立仓库 commit：**

```bash
git -C /c/Users/Moky/myproject/apps/android/apps/goodx add -A
git -C /c/Users/Moky/myproject/apps/android/apps/goodx commit -m "<简短描述>"
```

**禁止在没有 commit 的情况下做破坏性操作**（如 `git checkout --`、`git reset --hard`、`git clean -f`）。必须先 commit 保存当前状态。

平常只 commit 即可，**不用每次 push**。需要 push 时用户会说。

GoodX 为独立 Git 仓库，**不要操作父级 GameWorld 仓库**。

```text
仓库: https://github.com/mokyarcher/GoodX.git
分支: main
远程: git@github.com:mokyarcher/GoodX.git (SSH)
```

### 9. 不覆盖用户已有改动

项目当前存在较多未提交改动。修改文件前先读取目标文件。不要做无关格式化，不要大范围重写。

### 10. 回复格式

每次最终回复（非工具调用、非中间过程）的第一行必须以 **大哥** 开头。

### 11. 记录重要变更

完成较大功能、架构调整、体验优化后，应更新 `changelog.md`。不用写过细，但要能看懂：

- 改了什么
- 为什么改
- 采用了什么方案
- 是否有后续注意事项

---

## 常用命令

### 固定 Build / Verify 工作流

GoodX 默认采用轻量验证，避免无意义打包浪费时间和 token：

1. Android / Kotlin / Compose 改动：默认只运行 `:app:compileDebugKotlin --no-configuration-cache`。
2. 服务端 JS 改动：优先运行对应文件的 `node --check`，例如 `server/routes/admin.js`。
3. Retrofit / API 联动改动：Android 快速编译 + 服务端相关路由 `node --check`。
4. Gradle / Manifest / res 资源改动：可按需升级到更完整的 Debug 构建，但先说明原因。
5. 不主动运行 `assembleDebug`、不主动生成 APK、不主动安装；只有用户明确说“打包 / 生成 APK / assembleDebug / 装手机”才执行。

### Kotlin 编译检查

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
/c/Users/Moky/myproject/apps/android/apps/goodx/gradlew \
-p /c/Users/Moky/myproject/apps/android/apps/goodx \
:app:compileDebugKotlin --no-configuration-cache
```

### Debug APK 打包 / 发布（仅用户要求时）

**发布必须 clean + 禁用配置缓存，否则 versionCode 可能不更新：**

```bash
rm -rf /c/Users/Moky/myproject/apps/android/apps/goodx/.gradle/configuration-cache && \
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
/c/Users/Moky/myproject/apps/android/apps/goodx/gradlew \
-p /c/Users/Moky/myproject/apps/android/apps/goodx \
:app:clean :app:assembleDebug --no-configuration-cache
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 服务端语法检查

```bash
node --check server/routes/upload.js
node --check server/routes/goodItems.js
```

---

## 当前技术事实

- Android 使用 Kotlin + Compose + Material3
- 网络使用 Retrofit + OkHttp
- 图片使用 Coil
- 后端使用 Node.js + Express + MongoDB
- 图片处理使用 Sharp
- 线上服务器：`111.229.166.216:3002`
- 后端进程：`goodx-api`

---

## 已废弃内容

以下内容如果在代码或旧文档里出现，应视为历史遗留，除非用户明确重新启用：

- 圈子
- 邀请码
- 圈子成员管理
- 圈子权限
- 圈子好物列表
- APEX 红黑主视觉

---

## 文档维护约定

- `design.md`：产品和架构事实，随功能变化更新
- `agent.md`：AI / 开发助手工作规则，随用户偏好更新
- `changelog.md`：开发过程记录，记录重要变动和思路
