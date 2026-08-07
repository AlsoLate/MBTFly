# MBTFly 设计规范文档

> 代码风格、命名规范、文件组织、提交规范。

## 1. 代码风格

### 1.1 Java 代码
- 缩进：4 个空格（不使用 Tab）
- 行宽限制：无硬性限制，但建议不超过 120 字符
- 花括号：K&R 风格（左花括号不换行）
- import 顺序：标准库 → Minecraft/Fabric → 项目内部
- 每个文件以单个空行结尾

### 1.2 消息文本
- 游戏内消息统一使用前缀：`[Maple Client] [MBTFly] `
- 使用 Minecraft 颜色代码（section sign `§`）：
  - `§a` 绿色 — 成功/正常
  - `§b` 青色 — 坐标/数值
  - `§c` 红色 — 错误/警告
  - `§6` 金色 — 提示/倒计时
  - `§7` 灰色 — 说明/帮助
  - `§f` 白色 — 数值
- 使用 `Text.literal()` 创建消息文本

### 1.3 注释
- 类级别：使用 Javadoc 说明类的用途
- 方法级别：复杂逻辑须添加注释说明
- 行内注释：仅在必要时添加，说明"为什么"而非"做什么"
- 使用中文注释

## 2. 命名规范

### 2.1 包名
- 根包：`top.maple_bamboo_team.mbtfly`
- 子包按功能划分：`client`、`client.flight`、`mixin`、`util`

### 2.2 类名
- PascalCase 命名
- Mixin 类以 `Mixin` 后缀结尾
- 工具类以 `Utils` 后缀结尾

### 2.3 方法名
- camelCase 命名
- 布尔返回方法以 `is`/`has`/`should` 开头
- 事件处理方法以 `on` 开头

### 2.4 字段名
- 普通字段：camelCase
- 静态常量：UPPER_SNAKE_CASE
- Mixin `@Unique` 字段：camelCase

### 2.5 文件名
- Java 文件：PascalCase，与类名一致
- 配置文件：kebab-case 或点分隔
- 开发日志：`YYYY-MM-DD.md`

## 3. 文件组织

### 3.1 目录结构
```
项目根目录/
├── src/main/java/     — Java 源代码
├── src/main/resources/ — 资源文件 (fabric.mod.json, mixins 配置)
├── docs/               — 项目文档
├── devlog/             — 开发日志
├── CLAUDE.md           — AI 工作指引
├── build.gradle        — 构建脚本
├── gradle.properties   — 构建属性
└── settings.gradle     — Gradle 设置
```

### 3.2 文件归属
- 构建配置：根目录
- 源代码：`src/main/java/`
- 资源：`src/main/resources/`
- 文档：`docs/`
- 日志：`devlog/`

## 4. 提交规范

### 4.1 Commit Message 格式
```
<类型>: <简短描述>

<详细说明（可选）>
```

类型：
- `feat` — 新功能
- `fix` — 修复
- `refactor` — 重构
- `docs` — 文档变更
- `build` — 构建配置变更
- `chore` — 杂项

### 4.2 分支策略
- `main` — 稳定主分支
- 功能开发在独立分支进行，完成后合并

## 5. 变更记录规范

每次代码修改须记录：
1. **修改文件**: 文件路径
2. **修改内容**: 简述改了什么
3. **变更原因**: 为什么需要修改（API 变更、弃用、行为改变等）
4. **影响范围**: 是否影响其他文件/功能

## 6. UI 设计规范

- 消息文本使用 Minecraft 原生颜色代码
- 无自定义 GUI（纯命令行交互）
- 飞行统计使用分隔线美化输出
- 时间格式：`yyyy-MM-dd HH:mm:ss`（中文区域）
