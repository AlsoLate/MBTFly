# MBTFly 升级需求文档

> 从 Minecraft 1.21.1 升级至 1.21.11 的完整需求分析。
>
> **状态：升级已全部完成（2026-08-07 升级完成，2026-08-08 编译修复验证通过）**

## 1. 升级目标

| 项目 | 升级前版本 | 最终版本 | 数据来源/状态 |
|------|---------|---------|---------|
| Minecraft | 1.21.1 | 1.21.11 | 用户指定 — 已完成 |
| Yarn Mappings | 1.21.1+build.3 | 1.21.11+build.6 | Fabric Meta API — 已完成 |
| Fabric Loader | 0.16.9 | 0.19.3 | Fabric Meta API — 已完成 |
| Fabric API | 待确认 | 0.141.6+1.21.11 | Maven 仓库验证 — 已完成 |
| Fabric Loom | 1.10-SNAPSHOT | 1.17-SNAPSHOT (实际 1.17.19) | Gradle Plugin Portal — 已完成 |
| Gradle | 8.14.5 | 9.6.1 | Loom 1.17 要求 Gradle 9.5+ — 已完成 |
| Java | 21 | 21 (Zulu 21.0.10) | 无需变更 — 已完成 |

## 2. 当前项目状态分析

### 2.1 构建配置现状

`gradle.properties` 当前内容（已部分被修改为 1.21.11，但版本号不准确）：
- `minecraft_version=1.21.11` — 已更新但 Loader/API 版本不匹配
- `yarn_mappings=1.21.11+build.2` — 非最新（最新为 build.6）
- `loader_version=0.16.9` — 过旧（1.21.11 最新稳定版为 0.19.3）
- `fabric_version=0.105.0+1.21.11` — 版本号可疑（低于 1.21.8 的 0.131.0）

`build.gradle` 当前问题：
- 使用 `intermediary` 映射替代 Yarn 映射，但代码中使用 Yarn 命名
- `gradle.properties` 中定义了 `yarn_mappings` 但未被 `build.gradle` 引用

### 2.2 代码现状

项目包含 4 个 Java 源文件，使用的 MC/Fabric API：

| API/类 | 使用位置 | 变更风险 |
|--------|---------|---------|
| `ClientPlayerEntity` (Mixin 目标) | `ClientPlayerEntityMixin.java` | 低 — 核心类，通常稳定 |
| `ClientPlayerEntity.tick()` (Mixin 注入) | `ClientPlayerEntityMixin.java` | 中 — 方法签名可能变更 |
| `MinecraftClient` | 多处 | 低 — 核心类 |
| `MinecraftClient.options.*Key` | `ClientPlayerEntityMixin.java` | 中 — 按键绑定 API 可能变更 |
| `MinecraftClient.world.disconnect()` | `ClientPlayerEntityMixin.java` | 中 — 方法可能重命名/移除 |
| `MinecraftClient.disconnect()` | `ClientPlayerEntityMixin.java` | 中 — 方法可能重命名/移除 |
| `MinecraftClient.setScreen()` | `ClientPlayerEntityMixin.java` | 低 |
| `ClientCommandRegistrationCallback` | `MBTFlyClient.java` | 低 — Fabric API 稳定 |
| `ClientCommandManager` | `MBTFlyClient.java` | 低 — Fabric API 稳定 |
| `Vec3d` | 多处 | 低 — 核心数学类 |
| `Text.literal()` | 多处 | 低 |
| `TitleScreen` | `ClientPlayerEntityMixin.java` | 低 |
| `player.getPos()/getY()/getYaw()/getPitch()` | 多处 | 低 |
| `player.setYaw()/setPitch()` | `ClientPlayerEntityMixin.java` | 低 |

## 3. 核心需求

### 3.1 构建脚本更新
- 修正 `build.gradle` 的映射配置（从 `intermediary` 改回 Yarn）
- 更新 `gradle.properties` 中所有依赖版本至 1.21.11 对应的正确版本
- 验证 Fabric Loom 版本兼容性
- 验证 Fabric API 版本号

### 3.2 代码迁移
- 分析 1.21.1 → 1.21.11 之间的 API 变更（跨 10 个小版本）
- 修复所有因 API 变更导致的编译错误
- 验证 Mixin 注入目标方法签名是否变更

### 3.3 变更解释
- 每处修改须记录：原代码 → 新代码 → 变更原因
- 标注变更类型：API 重命名、方法移除、行为变更、弃用替换

## 4. 待确认事项（均已确认）

1. **Fabric API 版本**: ✅ 已确认 — `0.141.6+1.21.11`（Fabric Maven 仓库验证，构建解析成功）
2. **Fabric Loom 版本**: ✅ 已确认 — `1.17-SNAPSHOT`（实际解析 `1.17.19`），要求 Gradle 9.5+
3. **映射选择**: ✅ 已确认 — 使用 Yarn 映射（代码使用 Yarn 命名），不使用 `splitEnvironmentSourceSets()`（纯客户端项目，main source set 直接包含客户端类）
4. **Java 路径**: ✅ 已确认 — `C:/Program Files/Zulu/zulu-21`（本机实际安装的 Zulu JDK 21.0.10）
