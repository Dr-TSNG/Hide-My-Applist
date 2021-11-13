import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.gradle.BaseExtension
import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.PrintStream

val minSdkVer: Int by rootProject.extra
val targetSdkVer: Int by rootProject.extra
val buildToolsVer: String by rootProject.extra

val appVerName: String by rootProject.extra
val appVerCode: Int by rootProject.extra
val serviceVer: Int by rootProject.extra
val minRiruVer: Int by rootProject.extra
val minBackupVer: Int by rootProject.extra

val gitCommitCount: String by rootProject.extra
val gitCommitHash: String by rootProject.extra

val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android")
}

android {
    compileSdk = targetSdkVer
    buildToolsVersion = buildToolsVer

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.tsng.hidemyapplist"
        versionCode = appVerCode
        versionName = appVerName
        minSdk = minSdkVer
        targetSdk = targetSdkVer

        multiDexEnabled = false
        if (properties.getProperty("buildWithGitSuffix").toBoolean())
            versionNameSuffix = ".r${gitCommitCount}.${gitCommitHash}"

        buildConfigField("int", "SERVICE_VERSION", serviceVer.toString())
        buildConfigField("int", "MIN_RIRU_VERSION", minRiruVer.toString())
        buildConfigField("int", "MIN_BACKUP_VERSION", minBackupVer.toString())
    }

    signingConfigs.create("config") {
        storeFile = file(properties.getProperty("fileDir"))
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// This code is forked from LSPosed
// Make a class containing a byte array of signature
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
        val outSrc = file("$outSrcDir/com/tsng/hidemyapplist/Magic.java")
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
                    println("public final class Magic {")
                    print("public static final byte[] magicNumbers = {")
                    val bytes = certificateInfo.certificate.encoded
                    print(bytes.joinToString(",") { it.toString() })
                    println("};")
                    println("}")
                }
            }
        }
        variant.variantData.registerJavaGeneratingTask(signInfoTask, arrayListOf(outSrcDir))

        val kotlinCompileTask =
            tasks.findByName("compile${variant.name.capitalize()}Kotlin") as? SourceTask
        if (kotlinCompileTask != null) {
            kotlinCompileTask.dependsOn(signInfoTask)
            val srcSet = objects.sourceDirectorySet("magic", "magic").srcDir(outSrcDir)
            kotlinCompileTask.source(srcSet)
        }
    }
}

dependencies {
    implementation("com.drakeet.about:about:2.4.1")
    implementation("com.drakeet.multitype:multitype:4.3.0")
    implementation("com.scwang.smart:refresh-layout-kernel:2.0.3")
    implementation("com.scwang.smart:refresh-header-material:2.0.3")
    implementation("com.github.kyuubiran:EzXHelper:0.4.1")
    implementation("com.github.topjohnwu.libsu:core:3.1.2")

    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.fragment:fragment-ktx:1.4.0-alpha10")
    implementation("androidx.work:work-runtime-ktx:2.7.0")
    implementation("com.google.android.gms:play-services-ads:20.4.0")
    implementation("com.google.firebase:firebase-analytics-ktx:19.0.2")

    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
}