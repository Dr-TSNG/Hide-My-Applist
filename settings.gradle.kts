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

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application").version("7.1.1")
        id("com.android.library").version("7.1.1")
        id("org.jetbrains.kotlin.android").version("1.6.10")
    }
}

rootProject.name = "Hide My Applist"

include(":app")
