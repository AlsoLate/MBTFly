# MBTFly 技术规范文档

> 项目技术栈、依赖管理、代码架构、API 用法规范。

## 1. 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 (LTS) | 使用 Eclipse Adoptium (Temurin) 发行版 |
| Gradle | 通过 Loom 管理 | 使用 Fabric Loom Gradle 插件 |
| Minecraft | 1.21.11 (目标) | Fabric 平台 |
| Fabric Loader | 0.19.3 (目标) | 模组加载器 |
| Fabric API | 待确认 | Fabric 官方 API 库 |
| Yarn Mappings | 1.21.11+build.6 (目标) | Minecraft 反混淆映射 |
| Mixin | 通过 Fabric Loader 提供 | 字节码注入框架 |

## 2. 项目架构

```
top.maple_bamboo_team.mbtfly/
├── client/
│   ├── MBTFlyClient.java          — 入口点 (ClientModInitializer)
│   │   ├── 命令注册 (/mbtfly)
│   │   ├── 全局状态管理 (destination, startTime, detectionRange 等)
│   │   └── 飞行控制 (pause/resume/stop)
│   └── flight/
│       └── FlightControl.java     — 飞行开关 (enabled 标志)
├── mixin/
│   └── ClientPlayerEntityMixin.java — 核心 Mixin
│       ├── 注入 ClientPlayerEntity.tick() (HEAD)
│       ├── 飞行逻辑：朝向计算、前进、升降
│       ├── 到达检测：平面距离 + 总距离
│       ├── 自动退出：10 秒倒计时后断开连接
│       └── 飞行统计：时间、路程
└── util/
    └── AimingUtils.java           — 朝向计算工具
        ├── getYaw()  — 计算目标方向的 Yaw 角
        ├── getPitch() — 计算目标方向的 Pitch 角
        └── aimAt()   — 直接设置玩家朝向
```

## 3. API 使用规范

### 3.1 Mixin 使用
- 仅使用 `@Inject` 注入，不使用 `@Overwrite` 或 `@Redirect`
- 注入点：`ClientPlayerEntity.tick()` 的 `HEAD`
- Mixin 配置文件：`mbtfly.mixins.json`
- 兼容级别：`JAVA_21`

### 3.2 命令注册
- 使用 Fabric API v2 客户端命令 API
- 命令根节点：`/mbtfly`
- 子命令：`pause`、`resume`、`stop`、坐标参数

### 3.3 按键控制
- 通过 `MinecraftClient.options.*Key.setPressed()` 模拟按键
- 涉及按键：`forwardKey`、`backKey`、`leftKey`、`rightKey`、`jumpKey`、`sneakKey`
- 飞行时清除所有玩家输入，由模组完全接管

### 3.4 线程安全注意事项
- `autoExitCountdown` 在 tick 线程中递减，在新线程中执行退出操作
- 使用 `client.execute()` 将退出操作回调到主线程
- 全局状态使用 `static` 字段，须注意多线程访问

## 4. 依赖管理规范

### 4.1 版本统一管理
- 所有版本号定义在 `gradle.properties` 中
- `build.gradle` 通过 `project.*` 引用，不硬编码版本号
- 版本变更须同时更新 `fabric.mod.json` 中的依赖声明

### 4.2 映射配置
- 使用 Yarn 映射（`net.fabricmc:yarn`），不使用 intermediary
- `build.gradle` 中 mappings 行应为：
  ```groovy
  mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
  ```

## 5. 构建规范

### 5.1 Java 版本
- 源码兼容性：Java 21
- 目标字节码：Java 21
- `build.gradle` 中通过 `options.release = 21` 强制

### 5.2 编码
- 所有源文件使用 UTF-8 编码
- `processResources` 中 `filteringCharset = "UTF-8"`

### 5.3 资源处理
- `fabric.mod.json` 中的 `${version}`、`${minecraft_version}`、`${loader_version}` 在构建时替换
