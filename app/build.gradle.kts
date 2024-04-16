import java.util.*

val officialBuild: Boolean by rootProject.extra

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.autoresconfig)
    alias(libs.plugins.materialthemebuilder)
    alias(libs.plugins.refine)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.nav.safeargs.kotlin)
}

if (officialBuild) {
    plugins.apply(libs.plugins.gms.get().pluginId)
}

android {
    namespace = "com.tsng.hidemyapplist"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

kotlin {
    jvmToolchain(17)
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
    val variantCapped = variant.name.replaceFirstChar { it.titlecase(Locale.ROOT) }
    val variantLowered = variant.name.lowercase(Locale.ROOT)

    task<Sync>("build$variantCapped") {
        dependsOn("assemble$variantCapped")
        from(layout.buildDirectory.dir("outputs/apk/$variantLowered"))
        into(layout.buildDirectory.dir("apk/$variantLowered"))
        rename(".*.apk", "HMA-V${variant.versionName}-${variant.buildType.name}.apk")
    }
}

afterEvaluate {
    afterEval()
}

dependencies {
    implementation(projects.common)
    runtimeOnly(projects.xposed)

    implementation(platform(libs.com.google.firebase.bom))
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.com.drakeet.about)
    implementation(libs.com.drakeet.multitype)
    implementation(libs.com.github.kirich1409.viewbindingpropertydelegate)
    implementation(libs.com.github.liujingxing.rxhttp)
    implementation(libs.com.github.liujingxing.rxhttp.converter.serialization)
    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.google.android.material)
    implementation(libs.com.google.android.gms.play.services.ads)
    implementation(libs.com.google.firebase.analytics.ktx)
    implementation(libs.com.squareup.okhttp3)
    implementation(libs.dev.rikka.hidden.compat)
    implementation(libs.dev.rikka.rikkax.material)
    implementation(libs.dev.rikka.rikkax.material.preference)
    implementation(libs.me.zhanghai.android.appiconloader)
    compileOnly(libs.dev.rikka.hidden.stub)
    ksp(libs.com.github.liujingxing.rxhttp.compiler)
}

configurations.all {
    exclude("androidx.appcompat", "appcompat")
}
