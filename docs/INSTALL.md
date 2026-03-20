# ScalaBPE Navigator 编译、安装与调试指南

> 本文档涵盖环境准备、Gradle/Maven 双构建方式、本地调试、插件安装等完整流程。

---

## 目录

- [环境要求](#环境要求)
- [项目结构](#项目结构)
- [环境配置](#环境配置)
  - [配置 IntelliJ IDEA 路径](#配置-intellij-idea-路径)
  - [配置 Gradle JVM 路径](#配置-gradle-jvm-路径)
- [方式一：Gradle 构建（推荐）](#方式一gradle-构建推荐)
  - [编译](#gradle-编译)
  - [打包插件 ZIP](#gradle-打包插件-zip)
  - [本地调试（沙箱 IDEA）](#本地调试沙箱-idea)
  - [断点调试](#断点调试)
- [方式二：Maven 构建](#方式二maven-构建)
  - [Maven 环境](#maven-环境)
  - [Maven 配置说明](#maven-配置说明)
  - [Maven 编译命令](#maven-编译命令)
  - [Gradle 与 Maven 共存说明](#gradle-与-maven-共存说明)
- [安装插件](#安装插件)
  - [从 ZIP 安装（推荐）](#从-zip-安装推荐)
  - [从源码编译安装](#从源码编译安装)
- [版本兼容性](#版本兼容性)
- [常见问题](#常见问题)

---

## 环境要求

| 工具 | 要求 | 说明 |
|------|------|------|
| IntelliJ IDEA | **2025.2.x**（Ultimate 或 Community） | 用于本地调试运行，路径在 `build.gradle.kts` 中配置 |
| JDK | **IDEA 内置 JBR 21** | 无需单独安装，从 IDEA 安装目录中获取 |
| Gradle | **9.0+**（通过 Wrapper 自动管理） | 无需手动安装，`.\gradlew.bat` 自动使用 9.2.0 |
| Maven（可选） | **3.6+** | 仅用于编译，插件打包仍需 Gradle |

> **提示**：IDEA 2025.2.x 内置 JBR（JetBrains Runtime）版本为 JDK 21。插件开发推荐直接使用 IDEA 内置 JBR，避免 Windows 上 JDK 路径兼容性问题。

---

## 项目结构

```
scala-bpe-plugin/
├── build.gradle.kts                        # Gradle 构建配置
├── settings.gradle.kts                     # Gradle 设置（含阿里云镜像）
├── gradle.properties                       # Gradle 属性（JBR 路径、内存等）
├── pom.xml                                 # Maven 构建配置（可选）
├── gradle/wrapper/                         # Gradle Wrapper（自动下载 Gradle 9.2.0）
├── INSTALL.md                              # 本文档
├── USE_GUIDE.md                            # 功能使用指南
├── DEPLOY.md                               # 发布到 Marketplace 指南
└── src/main/
    ├── kotlin/com/shengqugames/bpe/
    │   ├── reference/                      # Ctrl+Click 导航引用
    │   ├── marker/                         # Gutter 图标
    │   ├── doc/                            # Hover 悬浮文档
    │   ├── completion/                     # 智能补全
    │   ├── navigation/                     # 导航处理器
    │   └── util/                           # 工具类（文件查找等）
    └── resources/
        ├── META-INF/plugin.xml             # 插件注册声明
        └── icons/                          # 自定义 SVG 图标
```

---

## 环境配置

### 配置 IntelliJ IDEA 路径

编辑 `build.gradle.kts`，将 `local(...)` 改为你本机 IDEA 的实际安装路径：

```kotlin
dependencies {
    intellijPlatform {
        local("E:/IntelliJ IDEA 2025.2.2")   // <-- 改为你的 IDEA 安装路径
    }
}
```

同时更新 `ideaVersion.sinceBuild` 与你的 IDEA 版本对应：

| IDEA 版本 | sinceBuild | jvmToolchain |
|-----------|-----------|--------------|
| 2025.2.x  | `252`     | `21`         |
| 2025.1.x  | `251`     | `21`         |
| 2024.3.x  | `243`     | `21`         |

### 配置 Gradle JVM 路径

编辑 `gradle.properties`，将路径改为你本机 IDEA 安装目录下的 `jbr` 子目录：

```properties
org.gradle.java.home=E:/IntelliJ IDEA 2025.2.2/jbr

# 可选：额外的 Java 安装路径（供 Gradle Toolchain 自动检测使用）
org.gradle.java.installations.paths=E:/IntelliJ IDEA 2025.2.2/jbr
```

验证 JBR 版本（应显示 JDK 21）：

```bat
"E:\IntelliJ IDEA 2025.2.2\jbr\bin\java.exe" -version
```

---

## 方式一：Gradle 构建（推荐）

Gradle 是 IntelliJ 插件开发的标准构建工具，支持完整的编译、打包、沙箱调试、发布流程。

### Gradle 编译

```bat
# 编译 Kotlin 源码（验证代码是否有错误）
.\gradlew.bat compileKotlin
```

### Gradle 打包插件 ZIP

```bat
# 打包插件为可安装的 ZIP
.\gradlew.bat buildPlugin
```

打包产物位于：

```
build/distributions/scala-bpe-plugin-1.0.1.zip
```

验证 ZIP 包内容：

```bat
tar -tf build/distributions/scala-bpe-plugin-1.0.1.zip
```

应包含类似结构：

```
scala-bpe-plugin/
  lib/
    scala-bpe-plugin-1.0.1.jar
    instrumented-scala-bpe-plugin-1.0.1.jar
    kotlin-stdlib-x.x.x.jar
    ...
```

其他常用命令：

```bat
# 验证 plugin.xml 结构是否正确
.\gradlew.bat verifyPluginStructure

# 清理构建产物
.\gradlew.bat clean
```

<!-- 截图：编译成功的终端输出 -->
> **[截图位置]** Gradle 编译成功示意

### 本地调试（沙箱 IDEA）

运行后会启动一个独立的沙盒 IDEA 实例，插件自动加载其中：

```bat
.\gradlew.bat runIde
```

**步骤：**

1. 等待沙盒 IDEA 窗口弹出（首次约 1-2 分钟，之后增量启动更快）
2. 在沙盒 IDEA 中通过 `File -> Open` 打开一个 ScalaBPE 项目
3. 打开 `avenue_conf/` 下的 XML 文件，查看 `<message>` 标签旁是否出现 Gutter 图标
4. 测试 Ctrl+Click 跳转、Hover 文档、智能补全等功能

<!-- 截图：runIde 启动的沙箱 IDEA 实例 -->
> **[截图位置]** 沙箱 IDEA 调试界面

### 断点调试

1. 在 IDEA 中打开本插件项目（`scala-bpe-plugin`）
2. 在需要调试的代码行设置断点
3. 在右侧 **Gradle 工具窗口** 找到 `Tasks -> intellij platform -> runIde`
4. **右键** `runIde` -> **Debug** 启动
5. 在弹出的沙盒 IDEA 中操作，断点会被命中

---

## 方式二：Maven 构建

项目同时提供了 `pom.xml`，支持使用 Maven 编译代码。

### Maven 环境

| 配置项 | 值 |
|--------|------|
| Maven 安装路径 | `E:\apache-maven-3.6.3` |
| 用户设置文件 | `E:\apache-maven-3.6.3\conf\settings.xml` |

### Maven 配置说明

`pom.xml` 中通过 `system` 作用域引用本地 IDEA 安装目录下的 JAR 文件作为编译依赖：

```xml
<properties>
    <!-- IntelliJ IDEA 本地安装路径，请根据实际情况修改 -->
    <idea.home>E:/IntelliJ IDEA 2025.2.2</idea.home>
</properties>
```

引用的核心 JAR 文件：

| JAR 文件 | 说明 |
|----------|------|
| `lib/app.jar` | 核心平台 API（PSI、VFS、Editor、Actions 等） |
| `lib/app-client.jar` | 客户端 API |
| `lib/lib.jar` | 平台库（工具类、集合、IO） |
| `lib/util.jar` / `util-8.jar` | 通用工具类 |
| `lib/product.jar` | 产品级 API |
| `lib/platform-loader.jar` | 平台加载器 |
| `lib/annotations.jar` | JetBrains 注解 |
| `lib/trove.jar` | Trove 集合库 |

> XML PSI（XmlTag、XmlToken 等）已内置于 `app.jar` 中，IDEA 2025.x 无需额外引用。

### Maven 编译命令

```bat
# 编译
E:\apache-maven-3.6.3\bin\mvn.cmd compile -s E:\apache-maven-3.6.3\conf\settings.xml

# 打包 JAR
E:\apache-maven-3.6.3\bin\mvn.cmd package -s E:\apache-maven-3.6.3\conf\settings.xml

# 清理
E:\apache-maven-3.6.3\bin\mvn.cmd clean -s E:\apache-maven-3.6.3\conf\settings.xml
```

如果已将 Maven 加入系统 PATH，可简写：

```bat
mvn compile
mvn package
mvn clean
```

### Gradle 与 Maven 共存说明

两套构建系统互不干扰，各自使用独立的配置和输出目录：

| 构建工具 | 配置文件 | 输出目录 | 能力范围 |
|----------|----------|----------|----------|
| **Gradle** | `build.gradle.kts` | `build/` | 编译、打包 ZIP、沙箱调试、发布 Marketplace |
| **Maven** | `pom.xml` | `target/` | 编译、打包 JAR |

> **注意**：Maven 只能编译代码和生成 JAR。完整的插件 ZIP 打包（含 `plugin.xml`、依赖库、资源文件）、沙箱调试（`runIde`）和 Marketplace 发布必须使用 Gradle。

---

## 安装插件

### 从 ZIP 安装（推荐）

1. 获取插件包 `scala-bpe-plugin-1.0.1.zip`（由开发者提供，或自行编译）
2. 打开 IntelliJ IDEA -> **Settings** -> **Plugins**
3. 点击右上角齿轮图标 -> **Install Plugin from Disk...**
4. 选择 `scala-bpe-plugin-1.0.1.zip` 文件
5. 点击 **OK** -> 重启 IDEA

<!-- 截图：IDEA 插件安装界面 -->
> **[截图位置]** IDEA Settings -> Plugins -> Install from Disk 操作示意

### 从源码编译安装

```bat
# 1. 克隆项目
git clone <仓库地址>
cd scala-bpe-plugin

# 2. 修改 build.gradle.kts 中的 IDEA 路径和 gradle.properties 中的 JBR 路径

# 3. 打包
.\gradlew.bat clean buildPlugin

# 4. 安装
# 将 build/distributions/scala-bpe-plugin-1.0.1.zip 通过 Install from Disk 安装
```

**运行环境要求**：
- IntelliJ IDEA 2025.2 及以上版本
- 项目中需包含标准的 ScalaBPE 目录结构：
  - `avenue_conf/` — XML 服务契约文件
  - `compose_conf/` — Scala/Flow 实现文件

---

## 版本兼容性

| IDEA 版本 | sinceBuild | 所需 Kotlin 版本 | JDK 版本 |
|-----------|-----------|-----------------|----------|
| 2025.2.x  | `252`     | 2.2.0+          | 21       |
| 2025.1.x  | `251`     | 2.1.x+          | 21       |
| 2024.3.x  | `243`     | 2.0.x / 2.1.x   | 21       |
| 2024.2.x  | `242`     | 2.0.x           | 21       |

修改兼容版本时需同步更新：
- `build.gradle.kts` 中的 `kotlin("jvm") version` 和 `sinceBuild`
- `pom.xml` 中的 `<kotlin.version>` 和 `<idea.home>`

---

## 常见问题

### Q: `runIde` 启动后看不到 Gutter 图标？

确认打开的 XML 文件满足：
- 根元素是 `<service name="..." id="...">`
- 子元素存在 `<message name="..." id="...">`
- 项目根目录下有对应的 `compose_conf/` 目录且包含匹配的实现文件

### Q: `instrumentCode` 任务报 `Packages does not exist` 错误？

Windows 上部分 JDK 发行版的兼容问题。解决方案：在 `gradle.properties` 中将 `org.gradle.java.home` 改为 IDEA 内置 JBR：

```properties
org.gradle.java.home=E:/IntelliJ IDEA 2025.2.2/jbr
```

### Q: 编译报 `incompatible version of Kotlin` 错误？

IDEA 版本与 Kotlin 编译器不兼容，参考上方 [版本兼容性](#版本兼容性) 表格修改 Kotlin 版本。

### Q: Gradle 报 TLS 握手失败 / 网络连接错误？

在项目 `gradle.properties` 中清除有问题的代理配置：

```properties
systemProp.https.proxyHost=
systemProp.https.proxyPort=
```

### Q: `Cannot find a Java installation matching languageVersion=21` 错误？

在 `gradle.properties` 中添加 JBR 路径：

```properties
org.gradle.java.installations.paths=E:/IntelliJ IDEA 2025.2.2/jbr
```

### Q: Maven 编译报找不到 system 依赖的 JAR？

确认 `pom.xml` 中 `<idea.home>` 属性指向了正确的 IDEA 安装路径，且该路径下存在 `lib/app.jar` 等文件。

### Q: 如何更新已安装的插件版本？

在 **Settings -> Plugins** 中卸载旧版本，然后重新从磁盘安装新的 ZIP 文件。
