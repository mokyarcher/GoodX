# GoodX - 更新日志

## 版本规范
采用 [SemVer](https://semver.org/)：`主版本.次版本.修订号`
- 主版本：重大架构变更或不兼容更新
- 次版本：新增功能（向下兼容）
- 修订号：Bug 修复

---

## [Unreleased]

### 计划中
- [ ] 服务器部署后端服务
- [ ] Android Retrofit API 接口
- [ ] 登录/注册页面 UI
- [ ] 圈子列表和创建页面
- [ ] 好物发布和浏览页面

---

## [0.1.0] - 2026-06-07

### 项目初始化
- ✅ 创建项目目录结构
- ✅ 编写 AGENTS.md（项目定义与文档规范）
- ✅ 编写 PLAN.md（开发计划与里程碑）
- ✅ 编写 CHANGELOG.md（版本日志模板）
- ✅ 编写 DEVLOG.md（开发日志模板）

### Android 骨架搭建
- ✅ 创建 Gradle 项目（GoodX）
- ✅ 配置版本目录（libs.versions.toml）
- ✅ 添加核心依赖（Compose、Navigation、Retrofit、Coil、Room）
- ✅ 创建数据模型（User、Circle、GoodItem + 8 分类）
- ✅ 创建 APEX 风格主题（无圆角、红黑配色）
- ✅ 配置 AndroidManifest.xml

### 后端骨架搭建
- ✅ 创建 Express 项目（goodx-server）
- ✅ 配置 MongoDB 连接
- ✅ 创建数据模型（User、Circle、GoodItem）
- ✅ 实现 JWT 认证中间件
- ✅ 实现用户认证 API（注册/登录）
- ✅ 实现圈子 API（创建/加入/列表）
- ✅ 实现好物 API（发布/列表/详情/点赞）

### 技术决策
- 确定技术栈：Kotlin + Compose + Node.js + MongoDB
- 确定包名：`team.sharex.goodx`
- 复用现有 download-site 部署体系
- 采用 MVVM + Repository 架构
