plugins {
    id("com.android.library")
    id("dev.rikka.tools.refine")
    kotlin("android")
}

android {
    namespace = "icu.nullptr.hidemyapplist.xposed"

    buildFeatures {
        buildConfig = false
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(projects.common)

    implementation("com.github.kyuubiran:EzXHelper:0.9.7")
    implementation("dev.rikka.hidden:compat:2.3.1")
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("dev.rikka.hidden:stub:2.3.1")
}
