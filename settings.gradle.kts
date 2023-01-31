pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "1.8.0"

        id("org.jetbrains.dokka") version "1.7.20"
        id("com.vanniktech.maven.publish") version "0.24.0"
    }
}

rootProject.name = "KotlinizedLruCache"
include(":lrucache")
