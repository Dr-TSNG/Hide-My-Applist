plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

val serviceVer: Int by rootProject.extra
val minExtensionVer: Int by rootProject.extra
val minBackupVer: Int by rootProject.extra

android {
    namespace = "icu.nullptr.hidemyapplist.common"

    defaultConfig {
        buildConfigField("int", "SERVICE_VERSION", serviceVer.toString())
        buildConfigField("int", "MIN_BACKUP_VERSION", minBackupVer.toString())
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
}
