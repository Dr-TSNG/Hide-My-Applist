import java.util.*

val officialBuild: Boolean by rootProject.extra

plugins {
    kotlin("android")
    kotlin("plugin.serialization")
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("dev.rikka.tools.autoresconfig")
    id("dev.rikka.tools.materialthemebuilder")
    id("dev.rikka.tools.refine")
    id("androidx.navigation.safeargs.kotlin")
}

if (officialBuild) {
    plugins.apply("com.google.gms.google-services")
}

android {
    namespace = "com.tsng.hidemyapplist"

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    applicationVariants.all {
        kotlin {
            sourceSets.getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}

autoResConfig {
    generateClass.set(true)
    generateRes.set(false)
    generatedClassFullName.set("icu.nullptr.hidemyapplist.util.LangList")
    generatedArrayFirstItem.set("SYSTEM")
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F",
            "Sakura" to "FF9CA8"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = true
}

fun afterEval() = android.applicationVariants.forEach { variant ->
    val variantCapped = variant.name.capitalize(Locale.ROOT)
    val variantLowered = variant.name.toLowerCase(Locale.ROOT)

    task<Sync>("build$variantCapped") {
        dependsOn("assemble$variantCapped")
        from("$buildDir/outputs/apk/$variantLowered")
        into("$buildDir/apk/$variantLowered")
        rename(".*.apk", "HMA-V${variant.versionName}-${variant.buildType.name}.apk")
    }
}

afterEvaluate {
    afterEval()
}

dependencies {
    implementation(projects.common)
    runtimeOnly(projects.xposed)

    val rxhttpVersion = "3.0.1"
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.drakeet.about:about:2.5.2")
    implementation("com.drakeet.multitype:multitype:4.3.0")
    implementation("com.github.kirich1409:viewbindingpropertydelegate:1.5.6")
    implementation("com.github.liujingxing.rxhttp:rxhttp:$rxhttpVersion")
    implementation("com.github.liujingxing.rxhttp:converter-serialization:$rxhttpVersion")
    implementation("com.github.topjohnwu.libsu:core:5.0.3")
    implementation("com.google.android.material:material:1.7.0")
    implementation("com.google.android.gms:play-services-ads:21.4.0")
    implementation("com.google.firebase:firebase-analytics-ktx:21.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("dev.rikka.hidden:compat:3.4.3")
    implementation("dev.rikka.rikkax.material:material:2.5.1")
    implementation("dev.rikka.rikkax.material:material-preference:2.0.0")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    compileOnly("dev.rikka.hidden:stub:3.4.3")
    ksp("com.github.liujingxing.rxhttp:rxhttp-compiler:$rxhttpVersion")
}

configurations.all {
    exclude("androidx.appcompat", "appcompat")
}
