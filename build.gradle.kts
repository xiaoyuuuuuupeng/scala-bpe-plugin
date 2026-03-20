plugins {
    id("org.jetbrains.intellij.platform") version "2.13.1"
    kotlin("jvm") version "2.2.0"
}

group = "com.shengqugames"
version = "1.0.0"

repositories {
    // 阿里云公共仓库（镜像 Maven Central + JCenter）
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

dependencies {
    intellijPlatform {
        local("E:/IntelliJ IDEA 2025.2.2")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.shengqugames.scala-bpe-plugin"
        name = "ScalaBPE Navigator"
        version = "1.0.0"
        ideaVersion {
            sinceBuild = "252"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
