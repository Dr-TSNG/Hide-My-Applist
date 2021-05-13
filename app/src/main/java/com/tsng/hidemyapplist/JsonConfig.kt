package com.tsng.hidemyapplist

import com.google.gson.Gson

class JsonConfig {
    class Template {
        val HideTWRP = false
        val HideAllApps = false
        val EnableAllHooks = false
        val ExcludeWebview = false
        val HideApps = setOf<String>()
        val ApplyHooks = setOf<String>()
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