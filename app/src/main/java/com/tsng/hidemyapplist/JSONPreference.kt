package com.tsng.hidemyapplist

import com.google.gson.Gson

class JSONPreference {
    class Template {
        val EnableAllHooks = false
        val ApplyHooks = setOf<String>()
        val HideAllApps = false
        val ExcludeWebview = false
        val HideApps = setOf<String>()
        val HideTWRP = false
    }

    var HookSelf = false
    var DetailLog = false
    var Scope = mapOf<String, String>()
    var Templates = mutableMapOf<String, Template>()

    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object {
        @JvmStatic
        fun fromJson(str: String): JSONPreference {
            return Gson().fromJson(str, JSONPreference::class.java)
        }
    }
}