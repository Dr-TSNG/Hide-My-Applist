import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.gradle.BaseExtension
import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.*

val kotlinVersion: String by rootProject.extra

plugins {
    id("com.android.application")
    kotlin("android")
}

fun String.execute(currentWorkingDir: File = file("./")): String {
    val byteOut = ByteArrayOutputStream()
    project.exec {
        workingDir = currentWorkingDir
        commandLine = split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

val gitCommitCount = "git rev-list HEAD --count".execute()
val gitCommitHash = "git rev-parse --verify --short HEAD".execute()

android {
    compileSdk = 30
    ndkVersion = "22.0.7026061"
    buildToolsVersion = "30.0.3"

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.tsng.hidemyapplist"
        versionNameSuffix = ".r${gitCommitCount}.${gitCommitHash}"
        minSdk = 24
        targetSdk = 30
        externalNativeBuild.cmake {
            cppFlags += "-std=c++20"
        }

        versionCode = 49
        versionName = "2.0"
        buildConfigField("int", "SERVICE_VERSION", "49")
        buildConfigField("int", "MIN_RIRU_VERSION", "23")
        buildConfigField("int", "MIN_BACKUP_VERSION", "49")
    }

    signingConfigs.create("config") {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        val filePath = File(properties.getProperty("fileDir"))
        storeFile = file(filePath)
        storePassword = properties.getProperty("storePassword")
        keyAlias = properties.getProperty("keyAlias")
        keyPassword = properties.getProperty("keyPassword")
    }

    buildTypes {
        signingConfigs.named("config").get().also {
            debug {
                signingConfig = it
            }
            release {
                signingConfig = it
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles("proguard-rules.pro")
            }
        }
    }

    externalNativeBuild.cmake {
        path = file("src/main/cpp/CMakeLists.txt")
        version = "3.10.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents.onVariants { v ->
    val variant: ApplicationVariantImpl =
        if (v is ApplicationVariantImpl) v
        else (v as AnalyticsEnabledApplicationVariant).delegate as ApplicationVariantImpl
    val variantCapped = variant.name.capitalize()
    val variantLowered = variant.name.toLowerCase()

    variant.outputs.forEach {
        it.outputFileName.set("V${it.versionName.get()}-${variant.buildType}.apk")
    }

    afterEvaluate {
        val app = rootProject.project(":app").extensions.getByName<BaseExtension>("android")
        val outSrcDir = file("$buildDir/generated/source/signInfo/${variantLowered}")
        val outSrc = file("$outSrcDir/com/tsng/hidemyapplist/SignInfo.java")
        val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
            dependsOn(":app:validateSigning${variantCapped}")
            outputs.file(outSrc)
            doLast {
                val sign = app.buildTypes.named(variantLowered).get().signingConfig
                outSrc.parentFile.mkdirs()
                val certificateInfo = KeystoreHelper.getCertificateInfo(
                    sign?.storeType,
                    sign?.storeFile,
                    sign?.storePassword,
                    sign?.keyPassword,
                    sign?.keyAlias
                )
                PrintStream(outSrc).apply {
                    println("package com.tsng.hidemyapplist;")
                    println("public final class SignInfo {")
                    print("public static final byte[] CERTIFICATE = {")
                    val bytes = certificateInfo.certificate.encoded
                    print(bytes.joinToString(",") { it.toString() })
                    println("};")
                    println("}")
                }
            }
        }
        variant.variantData.registerJavaGeneratingTask(signInfoTask, arrayListOf(outSrcDir))
    }
}

repositories {
    maven("https://jitpack.io")
    maven("https://api.xposed.info/")
}

dependencies {
    implementation("com.drakeet.about:about:2.4.1")
    implementation("com.drakeet.multitype:multitype:4.3.0")
    implementation("com.scwang.smart:refresh-layout-kernel:2.0.3")
    implementation("com.scwang.smart:refresh-header-material:2.0.3")
    implementation("com.github.kyuubiran:EzXHelper:0.3.3")
    implementation("com.github.topjohnwu.libsu:core:3.1.2")

    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.fragment:fragment-ktx:1.4.0-alpha04")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
}