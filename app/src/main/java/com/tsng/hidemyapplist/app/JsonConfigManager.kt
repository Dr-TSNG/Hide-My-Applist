package com.tsng.hidemyapplist.app


import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import icu.nullptr.hidemyapplist.common.JsonConfig
import java.io.File

object JsonConfigManager {

    val configFile = File("${appContext.filesDir.absolutePath}/config.json")
    val globalConfig: JsonConfig

    init {
        if (!configFile.exists())
            configFile.writeText(JsonConfig().toString())
        try {
            globalConfig = JsonConfig.parse(configFile.readText())
            val configVersion = globalConfig.configVersion
            if (configVersion < 49) throw RuntimeException("Config version too old")
            if (configVersion < 65) migrateFromPre65()
            globalConfig.configVersion = BuildConfig.VERSION_CODE
        } catch (e: Exception) {
            makeToast(R.string.config_damaged)
            throw RuntimeException("Config file too old or damaged").apply { addSuppressed(e) }
        }
    }

    @Synchronized
    private fun save() {
        configFile.writeText(globalConfig.toString())
    }

    @Synchronized
    fun edit(block: JsonConfig.() -> Unit) {
        globalConfig.block()
        save()
    }

    private fun migrateFromPre65() {
        globalConfig.templates.forEach { (_, data) ->
            JsonConfig.Template::class.java.getDeclaredField("queryParamRules").apply {
                isAccessible = true
                set(data, mutableSetOf<String>())
            }
        }
        globalConfig.scope.forEach { (_, data) ->
            JsonConfig.AppConfig::class.java.getDeclaredField("extraQueryParamRules").apply {
                isAccessible = true
                set(data, mutableSetOf<String>())
            }
        }
    }
}
