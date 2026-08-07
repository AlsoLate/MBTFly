# MBTFly 项目工作指引

> 本文件是 AI 辅助开发的核心指引文件，所有开发工作须遵循此文件中的规范和流程。

## 项目概述

- **项目名称**: MBTFly (Maple Bamboo Team Fly)
- **项目类型**: Minecraft Fabric 客户端模组
- **功能**: 自动飞行到指定坐标
- **许可证**: LGPL-3.0
- **开发团队**: Maple_Bamboo_Team

## 当前升级状态

| 项目 | 当前版本 | 目标版本 | 状态 |
|------|---------|---------|------|
| Minecraft | 1.21.1 | 1.21.11 | 待执行 |
| Yarn Mappings | 1.21.1+build.3 | 1.21.11+build.6 | 待执行 |
| Fabric Loader | 0.16.9 (gradle.properties 中) | 0.19.3 | 待执行 |
| Fabric API | 0.105.0+1.21.11 (待验证) | 待确认 | 待验证 |
| Fabric Loom | 1.10-SNAPSHOT | 待确认 | 待验证 |
| Java | 21 | 21 | 无需变更 |

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

## 已知问题（升级前须解决）

1. **映射不一致**: `build.gradle` 使用 `intermediary` 映射，但代码使用 Yarn 命名（如 `ClientPlayerEntity`、`MinecraftClient` 等），须改回 Yarn 映射
2. **Fabric API 版本待验证**: `gradle.properties` 中 `fabric_version=0.105.0+1.21.11` 版本号可能不正确，须通过构建验证
3. **Loader 版本过旧**: `gradle.properties` 中 `loader_version=0.16.9` 远低于 1.21.11 对应的最新稳定版 `0.19.3`
4. **Yarn 版本可更新**: `gradle.properties` 中 `yarn_mappings=1.21.11+build.2` 可更新至最新稳定版 `1.21.11+build.6`
5. **Java 路径硬编码**: `gradle.properties` 中 Java 路径硬编码为特定机器路径，可能影响跨环境构建
