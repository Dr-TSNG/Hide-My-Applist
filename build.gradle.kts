buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("com.google.gms:google-services:4.3.10")
    }
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
val targetSdkVer by extra(32)

val appVerName by extra("2.3.3")
val appVerCode by extra(74)
val serviceVer by extra(74)
val minExtensionVer by extra(35)
val minBackupVer by extra(65)

val gitCommitCount by extra("git rev-list HEAD --count".execute())
val gitCommitHash by extra("git rev-parse --verify --short HEAD".execute())

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
