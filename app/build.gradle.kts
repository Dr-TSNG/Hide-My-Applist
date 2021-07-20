import com.android.build.api.component.analytics.AnalyticsEnabledApplicationVariant
import com.android.build.api.variant.impl.ApplicationVariantImpl
import com.android.build.gradle.BaseExtension
import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Paths

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
        multiDexEnabled = false
        externalNativeBuild.cmake {
            cppFlags += "-std=c++20"
        }

        versionCode = 56
        versionName = "2.0.7"
        buildConfigField("int", "SERVICE_VERSION", "54")
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
        path("src/main/cpp/CMakeLists.txt")
        version = "3.10.2"
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

// This code is forked from QNotified
// Add some tricks to dex tail to prevent modification
fun execDexTail(dexPath: String): Boolean {
    val cl = URLClassLoader(
        arrayOf(
            Paths.get(
                rootProject.projectDir.absolutePath,
                "libs", "dex-ptm", "build", "classes", "java", "main"
            ).toUri().toURL()
        )
    )
    val time = cl.loadClass("cc.ioctl.dextail.HexUtils")
        .getMethod("getTimeAsByteArray").invoke(null) as ByteArray
    return cl.loadClass("cc.ioctl.dextail.Main").getMethod(
        "checkAndUpdateTail",
        String::class.java, ByteArray::class.java, Boolean::class.java, PrintStream::class.java
    ).invoke(null, dexPath, time, true, System.out) as Boolean
}

tasks.register("dexTailDebug") {
    doLast {
        println("dexTailDebug.doLast invoked")
        val dexSet = mutableSetOf<File>()
        val tmpPaths = arrayOf(
            "intermediates/dex/debug/out/classes.dex", //3.6.x, plain
            "intermediates/dex/debug/shrunkDex/classes.dex", //3.6.x, minify
            "intermediates/dex/debug/mergeDexDebug/classes.dex", //4.0.x single
            "intermediates/dex/debug/minifyDebugWithR8/classes.dex" //4.0.x minify
        )
        tmpPaths.forEach {
            File(project.buildDir, it).also { f ->
                if (f.exists()) dexSet.add(f)
            }
        }
        if (dexSet.isEmpty()) {
            throw RuntimeException("dex not found: we only support 3.6.x, 4.0.x and 4.1.x")
        }
        dexSet.forEach {
            if (!execDexTail(it.absolutePath)) {
                throw RuntimeException("dexTail returned false")
            }
        }
    }
    dependsOn(":dex-ptm:assemble")
}

tasks.register("dexTailRelease") {
    doLast {
        println("dexTailDebug.doLast invoked")
        val dexSet = mutableSetOf<File>()
        val tmpPaths = arrayOf(
            "intermediates/dex/release/out/classes.dex", //3.6.x, plain
            "intermediates/dex/release/shrunkDex/classes.dex", //3.6.x, minify
            "intermediates/dex/release/mergeDexRelease/classes.dex", //4.0.x single
            "intermediates/dex/release/minifyReleaseWithR8/classes.dex" //4.0.x minify
        )
        tmpPaths.forEach {
            File(project.buildDir, it).also { f ->
                if (f.exists()) dexSet.add(f)
            }
        }
        if (dexSet.isEmpty()) {
            throw RuntimeException("dex not found: we only support 3.6.x, 4.0.x and 4.1.x")
        }
        dexSet.forEach {
            if (!execDexTail(it.absolutePath)) {
                throw RuntimeException("dexTail returned false")
            }
        }
    }
    dependsOn(":dex-ptm:assemble")
}

tasks.configureEach {
    val dexTailDebug = tasks["dexTailDebug"]
    val dexTailRelease = tasks["dexTailRelease"]

    if (name == "assembleDebug") dependsOn(dexTailDebug)
    if (name == "mergeDexDebug") dexTailDebug.dependsOn(this)
    if (name.startsWith("minifyDebug")) dexTailDebug.mustRunAfter(this)
    when (name) {
        "stripDebugDebugSymbols",
        "dexBuilderDebug", "mergeExtDexDebug",
        "mergeLibDexDebug", "mergeProjectDexDebug",
        "shrinkDebugRes"
        -> dexTailDebug.mustRunAfter(this)
    }

    if (name == "assembleRelease") dependsOn(dexTailRelease)
    if (name == "mergeDexRelease") dexTailRelease.dependsOn(this)
    if (name.startsWith("minifyRelease")) dexTailRelease.mustRunAfter(this)
    when (name) {
        "stripReleaseDebugSymbols",
        "dexBuilderRelease", "mergeExtDexRelease",
        "mergeLibDexRelease", "mergeProjectDexRelease",
        "shrinkReleaseRes"
        -> dexTailRelease.mustRunAfter(this)
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