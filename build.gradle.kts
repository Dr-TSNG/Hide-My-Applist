import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.7.20"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.2")
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("com.google.gms:google-services:4.3.14")
    }
}

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    kotlin("android") apply false
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = java.io.ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

val minSdkVer by extra(24)
val targetSdkVer by extra(33)
val buildToolsVer by extra("32.0.0")

val appVerName by extra("3.0.5-Beta")
val serviceVerCode by extra(89)
val minBackupVerCode by extra(65)

val androidSourceCompatibility = JavaVersion.VERSION_11
val androidTargetCompatibility = JavaVersion.VERSION_11

val gitCommitCount = "git rev-list HEAD --count".execute().toInt()
val gitCommitHash = "git rev-parse --verify --short HEAD".execute()

val localProperties = Properties()
localProperties.load(file("local.properties").inputStream())
val officialBuild by extra(localProperties.getProperty("officialBuild", "false") == "true")

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

fun Project.configureBaseExtension() {
    extensions.findByType<BaseExtension>()?.run {
        compileSdkVersion(targetSdkVer)
        buildToolsVersion = buildToolsVer

        defaultConfig {
            minSdk = minSdkVer
            targetSdk = targetSdkVer
            versionCode = gitCommitCount
            versionName = appVerName
            if (localProperties.getProperty("buildWithGitSuffix").toBoolean())
                versionNameSuffix = ".r${gitCommitCount}.${gitCommitHash}"

            consumerProguardFiles("proguard-rules.pro")
        }

        val config = localProperties.getProperty("fileDir")?.let {
            signingConfigs.create("config") {
                storeFile = file(it)
                storePassword = localProperties.getProperty("storePassword")
                keyAlias = localProperties.getProperty("keyAlias")
                keyPassword = localProperties.getProperty("keyPassword")
            }
        }

        buildTypes {
            all {
                signingConfig = config ?: signingConfigs["debug"]
            }
            named("release") {
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }

        compileOptions {
            sourceCompatibility = androidSourceCompatibility
            targetCompatibility = androidTargetCompatibility
        }
    }

    extensions.findByType<ApplicationExtension>()?.run {
        buildTypes {
            named("release") {
                isShrinkResources = true
            }
        }
    }

    extensions.findByType<KotlinCompile>()?.run {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
}
