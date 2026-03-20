# ScalaBPE Navigator 使用指南

> 一款 IntelliJ IDEA 插件，为 ScalaBPE 框架开发提供 XML 契约与 Scala/Flow 代码之间的快速导航、文档悬浮提示和智能补全。

---

## 目录

- [功能概览](#功能概览)
- [前置条件](#前置条件)
- [功能详解](#功能详解)
  - [1. .flow 文件语法高亮](#1-flow-文件语法高亮)
  - [2. XML → 实现文件导航（Gutter 图标）](#2-xml--实现文件导航gutter-图标)
  - [3. XML → 调用点导航（Ctrl+Click）](#3-xml--调用点导航ctrlclick)
  - [4. 实现文件 → XML 反向导航](#4-实现文件--xml-反向导航)
  - [5. Invoke 调用 → XML 契约导航](#5-invoke-调用--xml-契约导航)
  - [6. Hover 悬浮文档](#6-hover-悬浮文档)
  - [7. Invoke 智能补全](#7-invoke-智能补全)
  - [8. XML 内 type 定义跳转](#8-xml-内-type-定义跳转)
- [常见问题](#常见问题)

> 编译、安装与调试请参阅 [INSTALL.md](INSTALL.md)  
> 发布到 Marketplace 请参阅 [DEPLOY.md](DEPLOY.md)

---

## 前置条件

为了获得最佳体验，请先安装 **Scala 官方插件**：

1. 打开 IntelliJ IDEA -> **Settings** -> **Plugins** -> **Marketplace**
2. 搜索 **Scala** -> 安装由 **JetBrains** 提供的官方插件
3. 重启 IDEA

> 安装 Scala 插件后，`.flow` 文件将自动获得 **Scala 语法高亮**（关键词、字符串、注释、类型等彩色显示）。  
> 未安装 Scala 插件时，插件的导航、补全等核心功能仍然可用，但 `.flow` 文件无语法高亮。

---

## 功能概览

| 功能 | 触发方式 | 说明 |
|------|----------|------|
| .flow 语法高亮 | 自动 | 安装 Scala 插件后 .flow 文件自动获得 Scala 语法高亮 |
| XML → 实现文件 | 点击 Gutter 图标 | 从 XML `<message>` 跳转到 .scala/.flow 实现文件 |
| XML → 调用点 | Ctrl+Click message name | 从 XML 跳转到 invoke 调用位置 |
| 实现 → XML | Ctrl+Click 类名 / Gutter 图标 | 从 Scala/Flow 文件反向跳转到 XML 契约 |
| Invoke → XML | Ctrl+Click 字符串 | 从 `"svcName.msgName"` 跳转到 XML 契约 |
| 悬浮文档 | Ctrl+Hover / Ctrl+Q | 显示请求/响应参数信息 |
| 智能补全 | 输入 `"` 后键入字符 | 自动补全 serviceName.messageName 并生成参数模板 |
| XML type 引用 | Ctrl+Click `type` 属性值 | 本文件内从 `type="类型名"` 跳到 `<type name="类型名"/>` |

---

## 功能详解

### 1. .flow 文件语法高亮

安装 Scala 官方插件后，`.flow` 文件将自动被识别为 Scala 文件，获得完整的语法高亮支持。

**高亮内容**：
- 关键词（`class`、`def`、`val`、`var`、`if`、`else`、`for`、`match` 等）
- 字符串和字符字面量
- 注释（单行 `//` 和多行 `/* */`）
- 数字字面量
- 类型和注解

> 未安装 Scala 插件时，`.flow` 文件显示为纯文本，不影响导航和补全功能。

<!-- 截图：.flow 文件的 Scala 语法高亮效果 -->
> **[截图位置]** .flow 文件语法高亮前后对比

---

### 2. XML → 实现文件导航（Gutter 图标）

在 XML 服务描述文件中，每个 `<message>` 标签左侧的行号栏会显示一个绿色的 **`{→}`** 图标。

**使用方法**：点击该图标，直接跳转到对应的 .scala 或 .flow 实现文件。

支持的命名约定：
- 文件名匹配：`serviceName.messageName.scala` / `.flow`
- 类名匹配：`Flow_servicename_messagename`
- Flow 注释匹配：`//$serviceName.messageName`

<!-- 📸 截图：XML 文件中 <message> 标签旁的 Gutter 图标 -->
> **[截图位置]** XML 文件中 Gutter 图标示意

<!-- 📸 截图：点击 Gutter 图标后跳转到 .scala 实现文件 -->
> **[截图位置]** 跳转到实现文件后的效果

---

### 3. XML → 调用点导航（Ctrl+Click）

在 XML 文件中，按住 **Ctrl** 并点击 `<message name="xxx">` 中的 `name` 属性值，可跳转到代码中 `invoke("serviceName.messageName", ...)` 的调用位置。

**使用方法**：按住 Ctrl + 左键点击 message 的 name 值。

- 若只有一个调用点，直接跳转
- 若有多个调用点，弹出选择列表

<!-- 📸 截图：Ctrl+Click message name 跳转到 invoke 调用位置 -->
> **[截图位置]** Ctrl+Click 跳转到 invoke 调用点

<!-- 📸 截图：多个调用点时的选择弹窗 -->
> **[截图位置]** 多目标选择弹窗

---

### 4. 实现文件 → XML 反向导航

在 .scala 或 .flow 文件中，支持从代码反向跳转到 XML 契约定义。

**触发条件**（满足任一即可）：
- 类名包含 `Flow_servicename_messagename` 模式
- 文件中包含 `//$serviceName.messageName` 注释

**使用方法**：
- **Ctrl+Click**：点击类名或注释，跳转到对应 XML `<message>` 标签
- **Gutter 图标**：点击行号旁的图标跳转

<!-- 📸 截图：Scala 文件中类名的 Ctrl+Click 反向跳转 -->
> **[截图位置]** 从 Scala 类名反向跳转到 XML

<!-- 📸 截图：Flow 文件中注释的反向跳转 -->
> **[截图位置]** 从 Flow 注释反向跳转到 XML

---

### 5. Invoke 调用 → XML 契约导航

在 .scala 或 .flow 文件中，`invoke("dbPayRouter.isExistOrderUserRecord", ...)` 中的字符串可直接 Ctrl+Click 跳转到 XML 契约。

**使用方法**：按住 Ctrl + 左键点击 `"serviceName.messageName"` 字符串。

<!-- 📸 截图：invoke 字符串的 Ctrl+Click 跳转 -->
> **[截图位置]** 从 invoke 字符串跳转到 XML 契约

---

### 6. Hover 悬浮文档

**快速预览（Ctrl+Hover）**：将鼠标悬停在 XML `<message>` 标签上时，显示服务名、方法名、ID 以及请求/响应参数摘要。

**详细文档（Ctrl+Q）**：按 Ctrl+Q 可查看完整的参数表格，包含字段名和类型。

<!-- 📸 截图：Ctrl+Hover 悬浮快速预览 -->
> **[截图位置]** Ctrl+Hover 快速参数预览

<!-- 📸 截图：Ctrl+Q 完整文档视图 -->
> **[截图位置]** Ctrl+Q 详细参数文档

---

### 7. Invoke 智能补全

在 .scala 或 .flow 文件中编写 `invoke` 调用时，输入引号后键入字符即可触发自动补全。

**特性**：
- 输入 `"dbPa` 即可列出匹配的服务方法，如 `dbPayRouter.insertPayCollectionInfo`
- 支持**模糊匹配**：输入 `dbPins` 也能匹配 `dbPayRouter.insertPayCollectionInfo`
- 每输入一个字符实时过滤候选列表
- 选中后自动填入完整的 `serviceName.messageName`
- 自动生成请求参数模板，支持 **Tab 键**逐个编辑参数值

<!-- 📸 截图：输入 "dbPa 后的补全候选列表 -->
> **[截图位置]** 自动补全候选列表

<!-- 📸 截图：选中补全项后生成的参数模板 -->
> **[截图位置]** 补全后的参数模板（Tab 切换编辑）

---

### 8. XML 内 type 定义跳转

在 `avenue_conf` 下的契约 XML 中，`<field name="x_sendCount" type="x_sendCount_type"/>` 等标签上的 **`type` 属性值**（自定义类型名）支持 **Ctrl+左键**，跳转到**本文件内**的类型定义：

```xml
<type name="x_sendCount_type" class="int" code="101"/>
```

- 仅在 **`avenue_conf/**/*.xml`** 中启用，避免误处理其它 XML
- **只解析当前文件**中的 `<type name="..."/>`，不跨其它 XML 文件
- 若本文件内存在多个同名 `<type name="..."/>`，将弹出列表供选择
- 找不到定义时为软引用，不标红报错

---

## 常见问题

### Q: 安装后 Gutter 图标没有出现？

确认你的项目包含 `avenue_conf/` 目录，且 XML 文件中有标准的 `<service>` / `<message>` 结构。插件需要能在 `compose_conf/` 目录下找到匹配的实现文件才会显示图标。

### Q: Ctrl+Click 没有反应？

1. 确认光标位于 `<message name="xxx">` 的 `name` 属性值上
2. 确认 `compose_conf/` 目录下存在对应的 `invoke("serviceName.messageName")` 调用

### Q: 如何更新插件版本？

在 **Settings → Plugins** 中卸载旧版本，然后重新从磁盘安装新的 ZIP 文件。

### Q: 补全没有触发？

确保光标在 `invoke(...)` 调用的引号内部，且已输入至少一个字符。补全仅在 `.scala` 和 `.flow` 文件中生效。
