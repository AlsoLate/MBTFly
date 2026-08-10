<div align="center">

# MBTFly

**Maple Bamboo Team Fly** — Minecraft Fabric 客户端模组，自动飞行到指定坐标

[![Mod Version](https://img.shields.io/badge/Mod%20Version-1.0.0-7c3aed?style=flat-square)](https://github.com/AlsoLate/MBTFly/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%20%7C%201.21.1-4a9c2e?style=flat-square)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.3-db6d28?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-f89820?style=flat-square)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-LGPL--3.0-blue?style=flat-square)](./LICENSE.txt)

</div>

---

## 简介

MBTFly 是一个 Minecraft Fabric 客户端模组，通过 Mixin 注入玩家 Tick，自动计算朝向并操控按键，实现到指定坐标的自动飞行。支持暂停/恢复、自定义检测范围、到达后自动退出等功能。

> 本项目升级自 [Harvey2433/MBTFly](https://github.com/Harvey2433/MBTFly) 1.20.4 版本，适配 Minecraft 1.21.11。

## 功能特性

- **自动飞行** — 输入坐标后自动计算 Yaw/Pitch 并操控玩家朝目标飞行
- **高度控制** — 自动拉升/下降以匹配目标 Y 坐标
- **暂停/恢复** — 飞行途中可随时暂停和恢复，暂停时间不计入统计
- **自定义检测范围** — 可指定到达判定半径（默认 1.6 格）
- **自动退出** — 到达目的地后倒计时 10 秒自动断开连接
- **鞘翅耐久预警** — 在末地飞行时检测鞘翅耐久，低于 7% 自动寻找最近着陆点
- **飞行数据统计** — 到达后输出玩家名、起止时间、总耗时、总路程

## 命令一览

所有命令均为客户端命令，前缀 `/mbtfly`。

| 命令 | 说明 |
|------|------|
| `/mbtfly` | 显示帮助菜单 |
| `/mbtfly <x> <y> <z>` | 飞行到指定坐标（默认检测范围 1.6 格） |
| `/mbtfly <x> <y> <z> <range>` | 飞行到指定坐标，自定义检测范围 |
| `/mbtfly <x> <y> <z> <range> quit` | 飞行到指定坐标，到达后 10 秒自动退出 |
| `/mbtfly <x> <y> <z> quit` | 飞行到指定坐标，到达后 10 秒自动退出（默认范围） |
| `/mbtfly <x> ~ <z>` | 飞行到指定 XZ 坐标，保持当前 Y 坐标 |
| `/mbtfly <x> ~ <z> <range>` | 同上，自定义检测范围 |
| `/mbtfly <x> ~ <z> <range> quit` | 同上，到达后自动退出 |
| `/mbtfly <x> ~ <z> quit` | 同上，到达后自动退出（默认范围） |
| `/mbtfly pause` | 暂停当前飞行 |
| `/mbtfly resume` | 恢复暂停的飞行 |
| `/mbtfly stop` | 停止当前飞行 |

> **注意**：本模组没有自动避障功能，建议在末地或下界顶层使用。

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/) 0.16.0+
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api)（推荐 0.141.0+）
3. 从 [Releases](https://github.com/AlsoLate/MBTFly/releases) 下载对应版本的 `.jar` 文件
4. 将 `.jar` 放入 `.minecraft/mods/` 目录
5. 启动游戏

## 分支结构

本仓库采用双分支模式维护不同 Minecraft 版本：

| 分支 | Minecraft 版本 | 维护者 | 说明 |
|------|---------------|--------|------|
| `main` | 1.21.11 | [@AlsoLate](https://github.com/AlsoLate) | 最新版本，主开发分支 |
| `1.21.1` | 1.21.1 | [@Doufu-tofu](https://github.com/Doufu-tofu) | 1.21.1 适配版本，通过 PR 协作 |

## 从源码构建

### 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21（推荐 [Zulu JDK 21](https://www.azul.com/downloads/)） |
| Gradle | 9.6.1（wrapper 已包含） |
| Fabric Loom | 1.17-SNAPSHOT |

### 构建步骤

```bash
# 克隆仓库（选择所需分支）
git clone -b main https://github.com/AlsoLate/MBTFly.git      # 1.21.11
git clone -b 1.21.1 https://github.com/AlsoLate/MBTFly.git    # 1.21.1

cd MBTFly

# 编译构建
./gradlew build

# 构建产物位于 build/libs/mbtfly-1.0.0.jar
```

## 技术栈

| 项目 | main 分支 (1.21.11) | 1.21.1 分支 |
|------|---------------------|-------------|
| Minecraft | 1.21.11 | 1.21.1 |
| Yarn Mappings | 1.21.11+build.6 | 1.21.1 |
| Fabric Loader | 0.19.3 | 0.16.0+ |
| Fabric API | 0.141.6+1.21.11 | 0.115.0+ |
| Gradle | 9.6.1 | — |
| Java | 21 | 21 |

## 项目结构

```
MBTFly/
├── src/main/java/top/maple_bamboo_team/mbtfly/
│   ├── client/
│   │   ├── MBTFlyClient.java          # 模组入口，命令注册
│   │   └── flight/FlightControl.java   # 飞行开关标志
│   ├── mixin/
│   │   └── ClientPlayerEntityMixin.java # 核心飞行逻辑 (Mixin)
│   └── util/
│       └── AimingUtils.java            # Yaw/Pitch 计算工具
├── src/main/resources/
│   ├── fabric.mod.json                 # Fabric 模组元数据
│   └── mbtfly.mixins.json             # Mixin 配置
├── docs/                               # 技术文档
├── devlog/                             # 开发日志
├── mbtfly-user-guide/                  # HTML 用户指南
├── build.gradle                        # Gradle 构建脚本
├── gradle.properties                   # 版本与依赖配置
└── settings.gradle                     # Gradle 设置
```

## 贡献者

| 贡献者 | 角色 |
|--------|------|
| [@AlsoLate](https://github.com/AlsoLate) | 1.21.11 升级与维护 |
| [@Doufu-tofu](https://github.com/Doufu-tofu) | 1.21.1 适配与维护 |
| [Harvey2433](https://github.com/Harvey2433) | 原始 1.20.4 版本作者 |
| 枫璃梦 | 原始项目作者 |

欢迎通过 Pull Request 贡献代码。请提交到对应版本的分支。

## 许可证

本项目基于 [LGPL-3.0](./LICENSE.txt) 许可证开源。

Copyright (c) 2025 枫璃梦
