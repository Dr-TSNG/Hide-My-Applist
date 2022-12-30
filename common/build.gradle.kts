plugins {
    id("com.android.library")
    id("dev.rikka.tools.refine")
    kotlin("android")
    kotlin("plugin.serialization")
}

val configVerCode: Int by rootProject.extra
val serviceVerCode: Int by rootProject.extra
val minBackupVerCode: Int by rootProject.extra

android {
    namespace = "icu.nullptr.hidemyapplist.common"

    defaultConfig {
        buildConfigField("int", "CONFIG_VERSION", configVerCode.toString())
        buildConfigField("int", "SERVICE_VERSION", serviceVerCode.toString())
        buildConfigField("int", "MIN_BACKUP_VERSION", minBackupVerCode.toString())
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    compileOnly("dev.rikka.hidden:stub:3.4.3")
}
