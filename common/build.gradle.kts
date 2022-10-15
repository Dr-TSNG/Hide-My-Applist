plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

val serviceVerCode: Int by rootProject.extra
val minBackupVerCode: Int by rootProject.extra

android {
    namespace = "icu.nullptr.hidemyapplist.common"

    defaultConfig {
        buildConfigField("int", "SERVICE_VERSION", serviceVerCode.toString())
        buildConfigField("int", "MIN_BACKUP_VERSION", minBackupVerCode.toString())
    }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}
