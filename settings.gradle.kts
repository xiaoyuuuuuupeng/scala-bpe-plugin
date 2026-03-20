pluginManagement {
    repositories {
        // 阿里云 Gradle 插件镜像（镜像 plugins.gradle.org）
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        // 阿里云公共仓库（镜像 Maven Central + JCenter）
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
    }
}

rootProject.name = "scala-bpe-plugin"
