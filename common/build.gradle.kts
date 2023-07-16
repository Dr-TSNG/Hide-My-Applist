plugins {
    alias(libs.plugins.agp.lib)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
}

val configVerCode: Int by rootProject.extra
val serviceVerCode: Int by rootProject.extra
val minBackupVerCode: Int by rootProject.extra

android {
    namespace = "icu.nullptr.hidemyapplist.common"

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("int", "CONFIG_VERSION", configVerCode.toString())
        buildConfigField("int", "SERVICE_VERSION", serviceVerCode.toString())
        buildConfigField("int", "MIN_BACKUP_VERSION", minBackupVerCode.toString())
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.kotlinx.serialization.json)
    compileOnly(libs.dev.rikka.hidden.stub)
}
