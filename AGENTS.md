# GoodX - 私有圈子好物分享 App

## 项目定位
面向私有圈子的全品类好物推荐平台。用户在小圈子内分享生活中的优质物品/内容，涵盖实物（电子产品、生活用品）和虚拟（游戏、电影、小说、软件等）。

## 核心特点
- **私有圈子**：基于邀请制的小圈子，非公开社区
- **全品类覆盖**：实物 + 虚拟，生活 + 娱乐 + 生产力工具
- **好物推荐**：不是社交平台，聚焦"值得分享的东西"
- **跨平台**：Android App（主端）+ 可选 Web 管理端

## 技术栈
- Android：Kotlin + Jetpack Compose + Material3
- 后端：Node.js + Express + MongoDB（或 Firebase）
- 部署：沿用现有 download-site 体系

## 文档体系

| 文档 | 用途 | 维护者 |
|------|------|--------|
| `AGENTS.md` | 项目背景、定位、技术栈、文档索引 | AI / 开发者 |
| `PLAN.md` | 开发计划、里程碑、功能清单 | AI / PM |
| `CHANGELOG.md` | 版本发布记录、功能变更 | AI / 开发者 |
| `DEVLOG.md` | 开发日志、技术决策、问题记录 | AI / 开发者 |

## 文档分工

### AGENTS.md（本文档）
- 项目愿景与定位
- 目标用户画像
- 核心功能概述
- 技术架构决策
- 文档索引与规范

### PLAN.md
- 里程碑规划（MVP → V1 → V2）
- 功能清单（Must have / Should have / Nice to have）
- 开发排期
- 技术选型理由

### CHANGELOG.md
- 版本号规则（SemVer）
- 每个版本的变更列表
- 发布日期
- 已知问题

### DEVLOG.md
- 每日/每次开发的记录
- 技术决策过程
- 遇到的问题与解决方案
- 踩坑记录

## 命名规范
- 包名：`team.sharex.goodx`
- 版本号：`x.y.z`（SemVer）
- 分支：`main` / `dev` / `feature/xxx`
