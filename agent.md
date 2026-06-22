# GoodX Agent 准则

本文件记录 AI / 开发助手参与 GoodX 开发时必须遵守的准则。后续可随项目推进持续更新。

---

## 工作前必读

首次进行 GoodX 相关开发前，阅读本文件（`agent.md`）形成上下文记忆；后续会话优先从记忆（memory）中读取这些准则，不再重复读取文件。

产品定位、功能、技术栈、交互经验等详细事实见 `design.md`；近期开发过程和重要变动见 `changelog.md`。需要时再查，不必每次必读。

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

当前 UI 方向见 `design.md` →「视觉风格」。核心要点：

- 淡青白背景
- 液态玻璃卡片
- Tiffany 青强调色 `#0ABAB5`
- 轻量圆角、柔和高光
- 不做重黑、重红、硬边风格

除非用户明确要求，不要回到早期 APEX 红黑风。

### 4. 服务端变更要考虑部署

服务端线上目录：`/opt/goodx`；PM2 进程：`goodx-api`。

**违禁词库部署路径**：`scp` 到 `/opt/goodx/config/sensitive-words.json`，不是 `/opt/goodx/server/config/`（本地和服务端目录结构不同）。

部署前优先备份关键文件。部署后检查 `/health` 和 `pm2 status goodx-api`。

### 5. 每次改动后必须 Git Commit

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

### 6. 不覆盖用户已有改动

项目当前存在较多未提交改动。修改文件前先读取目标文件。不要做无关格式化，不要大范围重写。

### 7. 回复格式

每次最终回复（非工具调用、非中间过程）的第一行必须以 **大哥** 开头。

### 8. 记录重要变更

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

**发布必须彻底清理缓存 + 禁用配置缓存，否则 versionCode 可能不更新：**

```bash
rm -rf /c/Users/Moky/myproject/apps/android/apps/goodx/.gradle \
  /c/Users/Moky/myproject/apps/android/apps/goodx/app/build \
  /c/Users/Moky/myproject/apps/android/apps/goodx/build && \
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
/c/Users/Moky/myproject/apps/android/apps/goodx/gradlew \
-p /c/Users/Moky/myproject/apps/android/apps/goodx \
:app:clean :app:assembleDebug --no-configuration-cache
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 发布检查清单（必须逐项确认）

1. **验证 APK 内部真实版本**
   - 不要只看 `app/build.gradle.kts`，必须读取 `AndroidManifest.xml` 确认 `versionCode` 和 `versionName`
   - 可用项目脚本：`python scripts/check_apk_version.py app/build/outputs/apk/debug/app-debug.apk`
   - 确认输出与目标版本一致后再继续

2. **使用唯一文件名部署**
   - 每个版本的 APK 文件名必须不同，例如 `goodx-v59.apk`
   - 禁止复用 `goodx.apk`，避免 ColorOS / FileProvider / 系统安装器缓存旧包
   - `/api/version` 的 `apkUrl` 必须指向带版本号的文件名，例如：
     ```text
     服务器IP:111.229.166.216
     http://111.229.166.216:3002/apk/goodx/goodx-v59.apk?v=59
     ```
3. **部署后验证**
   - 服务器 `/api/version` 返回的 `version`、`versionCode`、`apkUrl` 正确
   - 下载站 `https://www.sharex.team/download/goodx/` 版本信息已更新
   - `pm2 status goodx-api` / `pm2 status download-site` 状态正常

**历史教训：** 2026-06-18 发布 v0.7.5 时，因只清除了 `configuration-cache` 且沿用 `goodx.apk` 文件名，导致用户更新后系统提示「已安装相同版本 0.7.4」。后续发布必须严格执行本清单。

### 服务端语法检查

```bash
node --check server/routes/upload.js
node --check server/routes/goodItems.js
```

---

## 文档维护约定

- `design.md`：产品定位、功能、技术栈、架构、命名规范、交互经验、图片加载策略等事实，随功能变化更新
- `agent.md`：AI / 开发助手工作规则，随用户偏好更新
- `changelog.md`：开发过程记录，记录重要变动和思路
