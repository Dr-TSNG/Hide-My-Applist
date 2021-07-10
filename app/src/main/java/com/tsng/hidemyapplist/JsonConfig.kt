package com.tsng.hidemyapplist

import com.google.gson.Gson

class JsonConfig {
    data class Template(
        val isWhitelist: Boolean,
        val appList: MutableSet<String> = mutableSetOf(),
        val mapsRules: MutableSet<String> = mutableSetOf()
    )

    data class AppConfig(
        var useWhitelist: Boolean = false,
        var enableAllHooks: Boolean = false,
        var excludeSystemApps: Boolean = false,
        val applyHooks: MutableSet<String> = mutableSetOf(),
        val applyTemplates: MutableSet<String> = mutableSetOf(),
        val extraAppList: MutableSet<String> = mutableSetOf(),
        val extraMapsRules: MutableSet<String> = mutableSetOf(),
    )

    val configVersion = BuildConfig.VERSION_CODE
    var hookSelf = false
    var detailLog = false
    var maxLogSize = 512
    var templates = mutableMapOf<String, Template>()
    var scope = mutableMapOf<String, AppConfig>()

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object {
        @JvmStatic
        fun fromJson(str: String): JsonConfig {
            return Gson().fromJson(str, JsonConfig::class.java)
        }
    }
}