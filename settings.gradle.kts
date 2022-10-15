enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://api.xposed.info/")
    }
}

pluginManagement {
    val agpVersion: String by settings
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version agpVersion
        id("com.android.library") version agpVersion
        id("com.google.devtools.ksp") version "1.7.20-1.0.7"
        id("androidx.navigation.safeargs") version "2.5.0"
        id("dev.rikka.tools.autoresconfig") version "1.2.1"
        id("dev.rikka.tools.materialthemebuilder") version "1.3.3"
        id("dev.rikka.tools.refine") version "3.1.1"
        id("org.jetbrains.kotlin.android") version "1.7.20"
        kotlin("plugin.serialization") version "1.7.20"
    }
}

rootProject.name = "HideMyApplist"

include(
    ":app",
    ":common",
    ":xposed"
)
