import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.PrintStream
import java.util.Locale

plugins {
    id("com.android.library")
    id("dev.rikka.tools.refine")
    kotlin("android")
}

val agpVersion: String by project

android {
    namespace = "icu.nullptr.hidemyapplist.xposed"

    buildFeatures {
        buildConfig = false
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val variantCapped = variant.name.capitalize(Locale.ROOT)
        val variantLowered = variant.name.toLowerCase(Locale.ROOT)

        val outSrcDir = file("$buildDir/generated/source/signInfo/${variantLowered}")
        val outSrc = file("$outSrcDir/icu/nullptr/hidemyapplist/Magic.java")
        val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
            outputs.file(outSrc)
            doLast {
                val sign = android.buildTypes[variantLowered].signingConfig
                outSrc.parentFile.mkdirs()
                val certificateInfo = KeystoreHelper.getCertificateInfo(
                    sign?.storeType,
                    sign?.storeFile,
                    sign?.storePassword,
                    sign?.keyPassword,
                    sign?.keyAlias
                )
                PrintStream(outSrc).apply {
                    println("package icu.nullptr.hidemyapplist;")
                    println("public final class Magic {")
                    print("public static final byte[] magicNumbers = {")
                    val bytes = certificateInfo.certificate.encoded
                    print(bytes.joinToString(",") { it.toString() })
                    println("};")
                    println("}")
                }
            }
        }
        variant.registerJavaGeneratingTask(signInfoTask, outSrcDir)

        val kotlinCompileTask = tasks.findByName("compile${variantCapped}Kotlin") as KotlinCompile
        kotlinCompileTask.dependsOn(signInfoTask)
        val srcSet = objects.sourceDirectorySet("magic", "magic").srcDir(outSrcDir)
        kotlinCompileTask.source(srcSet)
    }
}

dependencies {
    implementation(projects.common)

    implementation("com.android.tools.build:apksig:$agpVersion")
    implementation("com.github.kyuubiran:EzXHelper:1.0.3")
    implementation("dev.rikka.hidden:compat:3.4.3")
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("dev.rikka.hidden:stub:3.4.3")
}
