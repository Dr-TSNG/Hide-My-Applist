package com.tsng.hidemyapplist

import com.google.gson.Gson

class JsonConfig {
    data class Template(
        val isWhitelist: Boolean,
        val appList: MutableSet<String> = mutableSetOf(),
        val mapsRules: MutableSet<String> = mutableSetOf(),
        val queryParamRules: MutableSet<String> = mutableSetOf()
    )

    data class AppConfig(
        var useWhitelist: Boolean = false,
        var enableAllHooks: Boolean = false,
        var excludeSystemApps: Boolean = false,
        val applyHooks: MutableSet<String> = mutableSetOf(),
        val applyTemplates: MutableSet<String> = mutableSetOf(),
        val extraAppList: MutableSet<String> = mutableSetOf(),
        val extraMapsRules: MutableSet<String> = mutableSetOf(),
        val extraQueryParamRules: MutableSet<String> = mutableSetOf()
    )

    var configVersion = BuildConfig.VERSION_CODE
    var detailLog = false
    var maxLogSize = 512
    val templates = mutableMapOf<String, Template>()
    val scope = mutableMapOf<String, AppConfig>()

    override fun toString(): String {
        return Gson().toJson(this)
    }

    fun clear() {
        templates.clear()
        scope.clear()
    }

    companion object {
        @JvmStatic
        fun fromJson(str: String): JsonConfig {
            return Gson().fromJson(str, JsonConfig::class.java)
        }
    }
}