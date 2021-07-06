package com.tsng.hidemyapplist

import com.google.gson.Gson

class JsonConfig {
    class Template {
        val isWhiteList = false
        val appList = setOf<String>()
        val mapsRules = setOf<String>()
    }

    class AppConfig {
        val useWhiteList = false
        val enableAllHooks = false
        val excludeSystemApps = false
        val applyHooks = setOf<String>()
        val applyTemplates = setOf<String>()
        val extraAppList = setOf<String>()
        val extraMapsRules = setOf<String>()
    }

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