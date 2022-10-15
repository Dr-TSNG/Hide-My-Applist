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

    implementation("com.github.kyuubiran:EzXHelper:1.0.3")
    implementation("dev.rikka.hidden:compat:3.4.2")
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("dev.rikka.hidden:stub:3.4.2")
}
