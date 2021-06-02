package com.tsng.hidemyapplist

import com.google.gson.Gson

class JsonConfig {
    class Template {
        val WhiteList = false
        val EnableAllHooks = false
        val ExcludeSystemApps = false
        val ApplyHooks = setOf<String>()
        val HideApps = setOf<String>()
        val MapsRules = setOf<String>()
    }

    var HookSelf = false
    var DetailLog = false
    var MaxLogSize = 512
    var Scope = mapOf<String, String>()
    var Templates = mutableMapOf<String, Template>()

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