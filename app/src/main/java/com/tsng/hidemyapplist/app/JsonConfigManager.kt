package com.tsng.hidemyapplist.app


import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import java.io.File


object JsonConfigManager {
    val configFile = File("${appContext.filesDir.absolutePath}/config.json")
    val globalConfig: JsonConfig

    private val configBak = File("${appContext.filesDir.absolutePath}/config.json.bak")
    private val configLck = File("${appContext.filesDir.absolutePath}/config.json.lck")

    init {
        if (configLck.exists()) {
            configBak.copyTo(configFile)
            configLck.delete()
        }
        if (!configFile.exists())
            configFile.writeText(JsonConfig().toString())
        try {
            globalConfig = JsonConfig.fromJson(configFile.readText())
        } catch (e: Exception) {
            makeToast(R.string.config_damaged)
            throw RuntimeException("Config file damaged").apply { addSuppressed(e) }
        }
    }

    @Synchronized
    private fun save() {
        configFile.writeText(globalConfig.toString())
    }

    @Synchronized
    fun edit(block: JsonConfig.() -> Unit) {
        configBak.writeText(globalConfig.toString())
        configLck.createNewFile()
        globalConfig.block()
        save()
        configLck.delete()
    }
}