// 根构建脚本
// AGP 9.0+ 内置 Kotlin 支持，无需 org.jetbrains.kotlin.android 插件。
// 但 AGP 9 默认绑定 KGP 2.2.10；为使用用户指定的 Kotlin 2.4.20，
// 通过 buildscript classpath 显式升级 KGP 与 Compose Compiler 插件。
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
