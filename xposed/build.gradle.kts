import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.PrintStream
import java.util.Locale

plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
}

android {
    namespace = "icu.nullptr.hidemyapplist.xposed"

    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    jvmToolchain(21)
}

afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val variantCapped = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
        val variantLowered = variant.name.lowercase(Locale.ROOT)

        val outSrcDir = layout.buildDirectory.dir("generated/source/signInfo/${variantLowered}")
        val outSrc = outSrcDir.get().file("icu/nullptr/hidemyapplist/Magic.java")
        val signInfoTask = tasks.register("generate${variantCapped}SignInfo") {
            outputs.file(outSrc)
            doLast {
                val sign = android.buildTypes[variantLowered].signingConfig
                outSrc.asFile.parentFile.mkdirs()
                val certificateInfo = KeystoreHelper.getCertificateInfo(
                    sign?.storeType,
                    sign?.storeFile,
                    sign?.storePassword,
                    sign?.keyPassword,
                    sign?.keyAlias
                )
                PrintStream(outSrc.asFile).apply {
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
        variant.registerJavaGeneratingTask(signInfoTask, outSrcDir.get().asFile)

        val kotlinCompileTask = tasks.findByName("compile${variantCapped}Kotlin") as KotlinCompile
        kotlinCompileTask.dependsOn(signInfoTask)
        val srcSet = objects.sourceDirectorySet("magic", "magic").srcDir(outSrcDir)
        kotlinCompileTask.source(srcSet)
    }
}

dependencies {
    implementation(projects.common)

    implementation(libs.com.android.tools.build.apksig)
    implementation(libs.com.github.kyuubiran.ezxhelper)
    implementation(libs.dev.rikka.hidden.compat)
    compileOnly(libs.de.robv.android.xposed.api)
    compileOnly(libs.dev.rikka.hidden.stub)
}
