pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application").version("7.1.0-alpha12")
        id("com.android.library").version("7.1.0-alpha12")
        id("org.jetbrains.kotlin.android").version("1.5.30")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jcenter.bintray.com")
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

rootProject.name = "Hide My Applist"

include(":app")
include(":dex-ptm")

val compilerLibsDir = File(settingsDir, "libs")
project(":dex-ptm").projectDir = File(compilerLibsDir, "dex-ptm")