# ScalaBPE Navigator 插件发布指南

> 本文档详细介绍如何将插件发布到 JetBrains Plugin Marketplace，让所有 IntelliJ IDEA 用户都可以通过 **Settings → Plugins → Marketplace** 搜索安装。

---

## 目录

- [发布前准备](#发布前准备)
- [第一步：注册 JetBrains 账号](#第一步注册-jetbrains-账号)
- [第二步：完善插件信息](#第二步完善插件信息)
- [第三步：打包插件](#第三步打包插件)
- [第四步：手动上传发布](#第四步手动上传发布)
- [第五步（可选）：配置 Gradle 自动发布](#第五步可选配置-gradle-自动发布)
- [版本更新发布](#版本更新发布)
- [审核说明](#审核说明)
- [常见问题](#常见问题)

---

## 发布前准备

确认以下事项已完成：

- [ ] 插件功能已测试通过
- [ ] `plugin.xml` 中的插件描述信息完整
- [ ] `build.gradle.kts` 中的版本号正确
- [ ] 执行 `.\gradlew.bat buildPlugin` 打包成功

---

## 第一步：注册 JetBrains 账号

1. 访问 [JetBrains Account](https://account.jetbrains.com/)
2. 点击 **Create JetBrains Account**，使用邮箱注册
3. 验证邮箱后登录

> 如果你已有 JetBrains 账号（购买过 IDEA License 等），可直接使用该账号。

---

## 第二步：完善插件信息

### 2.1 编辑 plugin.xml

打开 `src/main/resources/META-INF/plugin.xml`，确保以下信息完整：

```xml
<idea-plugin>
    <id>com.shengqugames.scala-bpe-plugin</id>
    <name>ScalaBPE Navigator</name>
    <vendor email="your-email@example.com" url="https://your-website.com">
        YourName / YourOrganization
    </vendor>

    <description><![CDATA[
    <h2>ScalaBPE Navigator</h2>
    <p>A navigation plugin for ScalaBPE framework development.</p>
    <ul>
        <li>Navigate from XML service contracts to Scala/Flow implementation files</li>
        <li>Navigate from code to XML contract definitions</li>
        <li>Hover documentation for request/response parameters</li>
        <li>Smart completion for invoke() calls with parameter templates</li>
    </ul>
    ]]></description>

    <change-notes><![CDATA[
    <h3>1.0.2</h3>
    <ul>
        <li>Initial release</li>
        <li>XML ↔ Scala/Flow bidirectional navigation</li>
        <li>Invoke call site navigation</li>
        <li>Hover documentation</li>
        <li>Smart completion with Live Template parameters</li>
    </ul>
    ]]></change-notes>

    <!-- ... extensions ... -->
</idea-plugin>
```

**关键字段说明**：

| 字段 | 必填 | 说明 |
|------|------|------|
| `<id>` | 是 | 插件唯一标识，发布后不可修改 |
| `<name>` | 是 | 插件显示名称，不能包含 "plugin" 一词 |
| `<vendor>` | 是 | 发布者信息（邮箱、网址、名称） |
| `<description>` | 是 | 插件描述，支持 HTML，至少 40 个字符 |
| `<change-notes>` | 建议 | 版本更新日志，支持 HTML |
| `<idea-version since-build>` | 是 | 兼容的最低 IDEA 版本 |

### 2.2 编辑 build.gradle.kts（可选增强）

可以在 `build.gradle.kts` 的 `pluginConfiguration` 中配置更多信息：

```kotlin
intellijPlatform {
    pluginConfiguration {
        id = "com.shengqugames.scala-bpe-plugin"
        name = "ScalaBPE Navigator"
        version = "1.0.2"
        ideaVersion {
            sinceBuild = "252"
            // untilBuild = "253.*"  // 可选：限制最高兼容版本
        }
    }
}
```

> **注意**：不设置 `untilBuild` 表示兼容所有未来版本，建议初期不设置。

---

## 第三步：打包插件

```bash
# 清理并打包
.\gradlew.bat clean buildPlugin
```

打包产物位于：

```
build/distributions/scala-bpe-plugin-1.0.2.zip
```

验证 ZIP 包内容：

```bash
# 查看 ZIP 内文件结构
tar -tf build/distributions/scala-bpe-plugin-1.0.2.zip
```

应包含类似结构：

```
scala-bpe-plugin/
  lib/
    scala-bpe-plugin-1.0.2.jar
    instrumented-scala-bpe-plugin-1.0.2.jar
    kotlin-stdlib-x.x.x.jar
    ...
```

---

## 第四步：手动上传发布

### 4.1 登录插件市场

1. 访问 [JetBrains Plugin Marketplace](https://plugins.jetbrains.com/)
2. 点击右上角 **Sign In**，使用 JetBrains 账号登录

### 4.2 上传插件

1. 登录后，点击右上角头像 → **Upload plugin**
2. 填写信息：

| 表单项 | 填写内容 |
|--------|----------|
| **Plugin ZIP file** | 选择 `build/distributions/scala-bpe-plugin-1.0.2.zip` |
| **License** | 选择合适的开源协议（如 Apache 2.0）或 Proprietary |
| **Category** | 选择 `Navigation` 或 `Languages` |
| **Tags** | 填入 `scala`, `bpe`, `navigation`, `xml` |
| **Source code URL** | 填写 Git 仓库地址（可选） |

3. 点击 **Upload** 提交

### 4.3 等待审核

- 首次发布需要 JetBrains 团队人工审核
- 审核周期通常为 **1-2 个工作日**
- 审核通过后会收到邮件通知
- 通过后插件将出现在 Marketplace 搜索结果中

---

## 第五步（可选）：配置 Gradle 自动发布

如果你希望通过命令行一键发布（CI/CD 友好），可以配置 Token 自动发布。

### 5.1 获取 Plugin Marketplace Token

1. 登录 [JetBrains Plugin Marketplace](https://plugins.jetbrains.com/)
2. 点击头像 → **My Tokens** (或访问 https://plugins.jetbrains.com/author/me/tokens)
3. 点击 **Generate Token**
4. 填写 Token 名称，点击 **Generate**
5. **立即复制并保存 Token**（只显示一次）

### 5.2 配置 build.gradle.kts

在 `build.gradle.kts` 中添加发布配置：

```kotlin
intellijPlatform {
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // channels = listOf("stable")       // 正式版
        // channels = listOf("beta")         // 测试版
        // channels = listOf("eap")          // 早期预览版
    }
}
```

### 5.3 设置环境变量

**Windows PowerShell**（临时）：

```powershell
$env:PUBLISH_TOKEN = "perm:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

**Windows 系统环境变量**（永久）：

```powershell
[System.Environment]::SetEnvironmentVariable("PUBLISH_TOKEN", "perm:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "User")
```

> **安全提醒**：Token 等同于你的发布密码，**绝对不要**提交到 Git 仓库中。建议将其存放在系统环境变量或 CI/CD Secret 中。

### 5.4 执行发布

```bash
.\gradlew.bat publishPlugin
```

发布成功后终端会输出确认信息，后续仍需等待审核。

---

## 版本更新发布

当你需要发布新版本时：

### 1. 更新版本号

修改 `build.gradle.kts`：

```kotlin
version = "1.1.0"  // 更新版本号

intellijPlatform {
    pluginConfiguration {
        version = "1.1.0"
    }
}
```

### 2. 更新 change-notes

在 `plugin.xml` 中添加新版本的更新日志：

```xml
<change-notes><![CDATA[
<h3>1.1.0</h3>
<ul>
    <li>新增 xxx 功能</li>
    <li>修复 xxx 问题</li>
</ul>
<h3>1.0.2</h3>
<ul>
    <li>Initial release</li>
</ul>
]]></change-notes>
```

### 3. 打包并发布

```bash
# 方式 A：手动上传
.\gradlew.bat clean buildPlugin
# 然后到 Marketplace 网站上传新版本 ZIP

# 方式 B：命令行自动发布（需已配置 Token）
.\gradlew.bat clean publishPlugin
```

> **更新版本无需重新审核**，上传后通常几分钟内即可在 Marketplace 生效。

---

## 审核说明

JetBrains 审核主要检查以下内容：

| 审核项 | 说明 |
|--------|------|
| 安全性 | 不包含恶意代码、不收集敏感数据 |
| 兼容性 | 与声明的 IDEA 版本兼容 |
| 质量 | 不会导致 IDEA 崩溃或严重性能问题 |
| 描述 | 插件名称和描述准确，不含误导性信息 |
| 命名 | 插件名不能包含 "IntelliJ"、"IDEA"、"plugin" 等保留词 |

**审核不通过怎么办？**

- JetBrains 会通过邮件告知原因
- 修改后重新上传即可
- 常见原因：描述太短、缺少 vendor 信息、兼容性声明有误

---

## 常见问题

### Q: 发布后用户多久能搜索到？

首次发布审核通过后即可搜索到。后续版本更新通常几分钟内生效。

### Q: 可以发布到多个 Channel 吗？

可以。默认 Channel 是 `stable`，你还可以发布到 `beta` 或 `eap`：
- **stable**：正式版，所有用户可见
- **beta**：测试版，用户需要手动添加 Beta Channel
- **eap**：早期预览版

使用方式：

```kotlin
intellijPlatform {
    publishing {
        channels = listOf("beta")
    }
}
```

用户添加 Beta Channel：**Settings → Plugins → ⚙ → Manage Plugin Repositories → 添加**：
```
https://plugins.jetbrains.com/plugins/beta/list
```

### Q: 如何让插件兼容更多 IDEA 版本？

调整 `plugin.xml` 中的 `since-build`：

| since-build | 兼容版本 |
|-------------|----------|
| `242` | IDEA 2024.2+ |
| `243` | IDEA 2024.3+ |
| `251` | IDEA 2025.1+ |
| `252` | IDEA 2025.2+ |

降低 `since-build` 可覆盖更多用户，但需要确保代码兼容对应版本的 API。

### Q: 如何查看插件的下载量和评价？

登录 Marketplace 后访问你的插件页面即可查看下载统计、评分和用户反馈。

### Q: Token 过期了怎么办？

在 Marketplace → **My Tokens** 页面重新生成即可，然后更新环境变量。

### Q: 可以删除已发布的版本吗？

可以。在 Marketplace 的插件管理页面中，选择对应版本点击 **Hide** 或 **Delete**。但建议隐藏而非删除，以防已安装用户受影响。
