# MBTFly 升级执行步骤

> 分阶段、增量式升级计划。每阶段完成后须确认再进入下一阶段。

## 阶段概览

| 阶段 | 名称 | 状态 | 风险等级 |
|------|------|------|---------|
| Phase 0 | 升级前准备与版本确认 | 已完成 | 低 |
| Phase 1 | 构建脚本更新 | 已完成 | 中 |
| Phase 2 | 依赖解析与编译验证 | 已完成 | 中 |
| Phase 3 | 代码迁移与 API 适配 | 已完成 | 高 |
| Phase 4 | 编译通过与功能验证 | 已完成 | 中 |
| Phase 5 | 最终清理与文档归档 | 已完成 | 低 |

---

## Phase 0: 升级前准备与版本确认

**目标**: 确认所有依赖版本号，记录当前状态

**任务清单**:
- [x] 分析项目当前结构和代码
- [x] 确认 Yarn Mappings 版本: `1.21.11+build.6` (Fabric Meta API)
- [x] 确认 Fabric Loader 版本: `0.19.3` (Fabric Meta API)
- [x] 确认 Fabric API 版本: `0.141.4+1.21.11` (Fabric Maven 仓库确认, 2026-05-11 发布)
- [x] 确认 Fabric Loom 版本: `1.17.17` (Maven 元数据确认, 需 Gradle 9.5+)
- [x] 确认映射配置修正方案 (intermediary → Yarn)
- [x] 确认 Java 路径在本机有效 — C:/Program Files/Zulu/zulu-21 (JDK 21.0.10)
- [x] 确认用户对升级方案的认可

**完成标准**: 所有版本号已确认，用户已认可方案

---

## Phase 1: 构建脚本更新

**目标**: 更新 `gradle.properties` 和 `build.gradle` 至 1.21.11

**任务清单**:
- [x] 修正 `build.gradle` 映射配置: `intermediary` → Yarn
- [x] 更新 `gradle.properties`:
  - `minecraft_version=1.21.11`
  - `yarn_mappings=1.21.11+build.6`
  - `loader_version=0.19.3`
  - `fabric_version=0.131.0+1.21.11` (待 Phase 2 验证)
- [x] 更新 Loom 插件版本 — 保持 1.10-SNAPSHOT (待 Phase 2 验证)
- [x] 更新 `fabric.mod.json` 中的依赖版本声明
- [x] 记录每处变更的原因

**变更记录**:

| 文件 | 变更 | 原因 |
|------|------|------|
| `build.gradle` L23-24 | `intermediary` → `yarn:${project.yarn_mappings}:v2` | 代码使用 Yarn 命名（ClientPlayerEntity 等），intermediary 映射会导致编译失败 |
| `gradle.properties` L10 | `yarn_mappings=1.21.11+build.2` → `1.21.11+build.6` | 更新至 Fabric Meta API 确认的最新稳定版 |
| `gradle.properties` L13 | `loader_version=0.16.9` → `0.19.3` | 0.16.9 过旧，0.19.3 是 1.21.11 对应的最新稳定版 |
| `gradle.properties` L16 | `fabric_version=0.105.0+1.21.11` → `0.131.0+1.21.11` | 0.105.0 版本号可疑（低于 1.21.8 的 0.131.0），暂定 0.131.0 待验证 |
| `gradle.properties` L22 | Java 路径 → `C:/Program Files/Zulu/zulu-21` | 原 Eclipse Adoptium 路径不存在，更新为机器上实际安装的 Zulu JDK 21 |
| `fabric.mod.json` L27,31 | `fabric-api >=0.105.0` → `>=0.131.0` | 与 gradle.properties 中的 fabric_version 保持一致 |

**完成标准**: 构建脚本版本号全部正确，配置一致

**回滚策略**: 恢复 `gradle.properties`、`build.gradle`、`fabric.mod.json` 至修改前状态

---

## Phase 2: 依赖解析与编译验证

**目标**: 验证 Gradle 能正确解析所有依赖

**任务清单**:
- [x] 执行 `gradlew --refresh-dependencies` 解析依赖
- [x] 验证 Yarn 映射下载成功
- [x] 验证 Fabric API 依赖解析成功 (修正版本: 0.131.0 → 0.141.4)
- [x] 验证 Minecraft 依赖解析成功
- [x] 记录依赖解析错误并修正版本号
- [x] 更新 Loom: 1.11.8 → 1.17.17 (解决 javadoc 兼容性错误)
- [x] 更新 Gradle: 8.14.5 → 9.6.1 (Loom 1.17 要求)

**完成标准**: 所有依赖解析成功，无 404 或版本不存在错误

**回滚策略**: 回退到 Phase 1 修改前的版本号

---

## Phase 3: 代码迁移与 API 适配

**目标**: 修复所有因 MC/Fabric API 变更导致的编译错误

**任务清单**:
- [x] 执行首次编译，收集所有编译错误 (共 11 个)
- [x] 逐个分析编译错误原因 (通过 `javap` 分析 Minecraft jar)
- [x] 修复 `MBTFlyClient.java` 中 9 处 `getPos()` → `getEntityPos()`
- [x] 修复 `ClientPlayerEntityMixin.java` 中 `getPos()` → `getEntityPos()`
- [x] 修复 `ClientPlayerEntityMixin.java` 中 `disconnect()` 签名变更 (2 处)
- [x] 验证 Mixin 注入目标方法签名正确 (`tick` 方法未变)
- [x] `AimingUtils.java` 无需修改 (无编译错误)
- [x] 记录每处代码变更的原因

**关注点**:
- `ClientPlayerEntity.tick()` 方法签名是否变更
- `MinecraftClient.world.disconnect()` / `client.disconnect()` 是否存在
- `client.options.*Key` 按键绑定 API 是否变更
- `TitleScreen` 构造函数是否变更
- Fabric API v2 客户端命令 API 是否有 breaking change

**完成标准**: 所有编译错误已修复，编译通过

**回滚策略**: 恢复所有 Java 源文件至修改前状态

---

## Phase 4: 编译通过与功能验证

**目标**: 确保模组能成功构建并生成 JAR

**任务清单**:
- [x] 执行完整构建 `gradlew build` — BUILD SUCCESSFUL
- [x] 验证 JAR 文件生成: `mbtfly-1.0.0.jar` (13,357 bytes)
- [x] 验证 sources JAR 生成: `mbtfly-1.0.0-sources.jar` (7,926 bytes)
- [x] Mixin 配置正确 (编译和 remapJar 均通过)
- [x] 生成 Gradle Wrapper (gradlew.bat + gradlew)

**完成标准**: `gradlew build` 成功，生成可用的 JAR 文件

**回滚策略**: 回退到 Phase 3 完成时的状态

---

## Phase 5: 最终清理与文档归档

**目标**: 清理临时文件，更新所有文档

**任务清单**:
- [x] 创建最终开发日志 `devlog/2026-08-07.md`
- [x] 更新 `docs/execution-plan.md` 标记所有阶段完成
- [x] 生成 Gradle Wrapper

**完成标准**: 所有文档已更新，项目处于干净状态

---

## 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| Fabric API 版本号不正确 | 中 | 高 | 通过 Maven 仓库验证，构建时自动报错 |
| Mixin 注入目标方法变更 | 低 | 高 | 编译时检查方法签名，运行时 Mixin 会报错 |
| 按键绑定 API 变更 | 低 | 中 | 编译时检查，查阅 Yarn changelog |
| Yarn 映射中类名重命名 | 低 | 中 | 编译时检查，查阅 Yarn diff |
| Loom 版本不兼容 | 低 | 高 | 使用最新稳定版 Loom |

## 回滚总策略

每个 Phase 都有独立的回滚策略。如果回滚后仍无法解决：
1. 使用 Git 恢复到升级前的 commit
2. 重新分析失败原因
3. 调整方案后重新执行
