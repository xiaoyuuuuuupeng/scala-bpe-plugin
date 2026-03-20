# ScalaBPE Navigator

适用于 [ScalaBPE 框架](https://github.com/shengqugames/scalabpe) 的 IntelliJ IDEA 开发辅助插件，帮助开发者在 BPE 项目中快速导航。

## 功能

### 已实现

- **XML → .flow 跳转**：在服务描述文件（`avenue_conf/*.xml`）中，对 `<message>` 标签：
  - **Ctrl+Click** `name` 属性值 → 跳转到对应的 `.flow` 流程文件
  - **Gutter 图标**（行号旁的 → 箭头）→ 点击跳转到对应的 `.flow` 流程文件

### 路径规律

| 文件类型 | 路径规律 | 示例 |
|---------|---------|------|
| 服务描述文件 | `avenue_conf/{serviceName}.xml` | `avenue_conf/service999.xml` |
| 流程文件 | `compose_conf/simpleflows/{serviceName}/{messageName}_{messageId}.flow` | `compose_conf/simpleflows/service999/queryUserInfo_1.flow` |

---

## 文档导航

| 文档 | 内容 |
|------|------|
| [INSTALL.md](docs/INSTALL.md) | 环境配置、Gradle/Maven 编译、打包、本地调试、安装 |
| [USE_GUIDE.md](docs/USE_GUIDE.md) | 插件功能详解与使用方法 |
| [DEPLOY.md](docs/DEPLOY.md) | 发布到 JetBrains Plugin Marketplace |

---

## 快速开始

```bat
# 1. 修改 build.gradle.kts 中的 IDEA 路径和 gradle.properties 中的 JBR 路径
# 2. 编译
.\gradlew.bat compileKotlin

# 3. 打包插件 ZIP
.\gradlew.bat buildPlugin

# 4. 本地调试
.\gradlew.bat runIde
```

> 详细步骤请参阅 [INSTALL.md](docs/INSTALL.md)

---

## 效果演示

以 `avenue_conf/service999.xml` 为例：

```xml
<service name="service999" id="999">

    <!-- 行号旁有绿色 {→} 图标，点击跳转到实现文件 -->
    <!-- Ctrl+Click name 值跳转到 invoke 调用点 -->
    <message name="queryUserInfo" id="1">
        <requestParameter>
            <field name="userId" type="userId_type"/>
        </requestParameter>
    </message>

</service>
```
