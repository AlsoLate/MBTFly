# MBTFly 项目工作指引

> 本文件是 AI 辅助开发的核心指引文件，所有开发工作须遵循此文件中的规范和流程。

## 项目概述

- **项目名称**: MBTFly (Maple Bamboo Team Fly)
- **项目类型**: Minecraft Fabric 客户端模组
- **功能**: 自动飞行到指定坐标
- **许可证**: LGPL-3.0
- **开发团队**: Maple_Bamboo_Team

## 当前升级状态

> 1.21.1 → 1.21.11 升级已全部完成（2026-08-07），2026-08-08 完成编译修复与验证。

| 项目 | 当前版本 | 目标版本 | 状态 |
|------|---------|---------|------|
| Minecraft | 1.21.11 | 1.21.11 | 已完成 |
| Yarn Mappings | 1.21.11+build.6 | 1.21.11+build.6 | 已完成 |
| Fabric Loader | 0.19.3 | 0.19.3 | 已完成 |
| Fabric API | 0.141.6+1.21.11 | 0.141.6+1.21.11 | 已完成 |
| Fabric Loom | 1.17-SNAPSHOT (实际 1.17.19) | 1.17-SNAPSHOT | 已完成 |
| Gradle | 9.6.1 (wrapper) | 9.6.1 | 已完成 |
| Java | 21 (Zulu 21.0.10) | 21 | 已完成 |

## 标准文件路径

### 文档目录 (`docs/`)

| 文件 | 说明 |
|------|------|
| `docs/README.md` | 文档索引，列出所有文档文件 |
| `docs/upgrade-requirements.md` | 升级需求文档，包含需求分析、版本对照、变更范围 |
| `docs/technical-specs.md` | 技术规范文档，包含技术栈、依赖版本、代码架构、API 用法 |
| `docs/design-standards.md` | 设计规范文档，包含代码风格、命名规范、提交规范 |
| `docs/execution-plan.md` | 执行步骤文档，包含分阶段升级计划、风险评估、回滚策略 |

### 开发日志目录 (`devlog/`)

| 文件 | 说明 |
|------|------|
| `devlog/README.md` | 开发日志说明，包含格式规范 |
| `devlog/YYYY-MM-DD.md` | 每日开发日志，记录完成事项和待办事项 |

### 源代码目录 (`src/main/java/top/maple_bamboo_team/mbtfly/`)

| 文件 | 说明 |
|------|------|
| `client/MBTFlyClient.java` | 模组入口，注册命令，管理全局状态 |
| `client/flight/FlightControl.java` | 飞行开关标志 |
| `mixin/ClientPlayerEntityMixin.java` | 核心飞行逻辑，Mixin 注入玩家 tick |
| `util/AimingUtils.java` | Yaw/Pitch 计算工具类 |

### 构建配置

| 文件 | 说明 |
|------|------|
| `build.gradle` | Gradle 构建脚本 |
| `gradle.properties` | Gradle 属性配置（版本号、Java 路径等） |
| `settings.gradle` | Gradle 设置脚本 |
| `src/main/resources/fabric.mod.json` | Fabric 模组元数据 |
| `src/main/resources/mbtfly.mixins.json` | Mixin 配置 |

## 工作流程

### 1. 开发前准备
- 阅读 `docs/upgrade-requirements.md` 了解当前升级需求
- 阅读 `docs/execution-plan.md` 了解当前执行阶段
- 查阅最近一次 `devlog/` 日志了解进度

### 2. 开发中规范
- 遵循 `docs/design-standards.md` 中的代码风格和命名规范
- 遵循 `docs/technical-specs.md` 中的技术规范
- 每完成一个步骤，更新 `docs/execution-plan.md` 中的状态
- 代码修改后须验证编译通过

### 3. 开发后记录
- 在 `devlog/` 中创建当日日志（格式: `YYYY-MM-DD.md`）
- 记录当日完成事项、待办事项、遇到的问题
- 更新 `docs/execution-plan.md` 中的进度状态

## 升级工作原则

1. **稳定优先**: 每一步修改后须验证编译通过，不一次性做过多修改
2. **增量推进**: 分阶段执行，每阶段完成后确认再进入下一阶段
3. **可回滚**: 每次重大修改前确认 Git 状态，确保可回滚
4. **文档同步**: 代码变更与文档更新同步进行
5. **变更解释**: 每处关键修改须说明原因（API 变更、弃用、行为改变等）

## 历史问题（升级期间已全部解决）

1. **映射不一致**（已解决）: `build.gradle` 曾使用 `intermediary` 映射，已改回 Yarn 映射
2. **Fabric API 版本**（已解决）: 经 Maven 仓库验证，最终确认 `0.141.6+1.21.11`
3. **Loader 版本过旧**（已解决）: 已升级至 `0.19.3`
4. **Yarn 版本可更新**（已解决）: 已更新至 `1.21.11+build.6`
5. **Java 路径硬编码**（已解决）: 2026-08-08 修正为 `C:/Program Files/Zulu/zulu-21`（原 Eclipse Adoptium 路径在本机不存在）

## 当前注意事项（2026-08-08 编译修复）

1. **不启用 `splitEnvironmentSourceSets()`**: 项目源码全部位于 `src/main/java` 且均为客户端代码，移除该配置后 `main` source set 才能引用 `net.minecraft.client.*` 与 Fabric client API
2. **Yarn 1.21.2+ API 变更**: `getPos()` → `getEntityPos()`、`getWorld()` → `getEntityWorld()`、`Identifier` 构造函数为 private（须用 `Identifier.of()`）、`disconnect()` 需要 `Text` 参数
3. **Gradle 9.6.1**: 本地缓存完整；Gradle 9.5 发行版下载 404 且缓存不完整，勿回退
