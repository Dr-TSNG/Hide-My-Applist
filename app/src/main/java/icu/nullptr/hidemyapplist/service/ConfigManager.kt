package icu.nullptr.hidemyapplist.service

import android.os.Build
import android.util.Log
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.common.BuildConfig
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.ui.util.makeToast
import java.io.File

object ConfigManager {

    data class TemplateInfo(val name: String?, val isWhiteList: Boolean)

    private const val TAG = "ConfigManager"
    private lateinit var config: JsonConfig
    val configFile = File("${hmaApp.filesDir.absolutePath}/config.json")

    fun init() {
        if (!configFile.exists()) {
            configFile.writeText(JsonConfig().toString())
        }
        runCatching {
            config = JsonConfig.parse(configFile.readText())
            val configVersion = config.configVersion
            if (configVersion < 65) throw RuntimeException("Config version too old")
            config.configVersion = BuildConfig.CONFIG_VERSION
            saveConfig()
        }.onFailure {
            makeToast(R.string.config_damaged)
            throw RuntimeException("Config file too old or damaged", it)
        }
    }

    private fun saveConfig() {
        val text = config.toString()
        configFile.writeText(text)
        ServiceClient.syncConfig(text)
    }

    var detailLog: Boolean
        get() = config.detailLog
        set(value) {
            config.detailLog = value
            saveConfig()
        }

    var maxLogSize: Int
        get() = config.maxLogSize
        set(value) {
            config.maxLogSize = value
            saveConfig()
        }

    var forceMountData: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) config.forceMountData
            else false
        set(value) {
            config.forceMountData = value
            saveConfig()
        }

    fun importConfig(json: String) {
        config = JsonConfig.parse(json)
        config.configVersion = BuildConfig.CONFIG_VERSION
        saveConfig()
    }

    fun hasTemplate(name: String?): Boolean {
        return config.templates.containsKey(name)
    }

    fun getTemplateList(): MutableList<TemplateInfo> {
        return config.templates.mapTo(mutableListOf()) { TemplateInfo(it.key, it.value.isWhitelist) }
    }

    fun getTemplateAppliedAppList(name: String): ArrayList<String> {
        return config.scope.mapNotNullTo(ArrayList()) {
            if (it.value.applyTemplates.contains(name)) it.key else null
        }
    }

    fun getTemplateTargetAppList(name: String): ArrayList<String> {
        return ArrayList(config.templates[name]?.appList ?: emptyList())
    }

    fun deleteTemplate(name: String) {
        config.scope.forEach { (_, appInfo) ->
            appInfo.applyTemplates.remove(name)
        }
        config.templates.remove(name)
        saveConfig()
    }

    fun renameTemplate(oldName: String, newName: String) {
        if (oldName == newName) return
        config.scope.forEach { (_, appInfo) ->
            if (appInfo.applyTemplates.contains(oldName)) {
                appInfo.applyTemplates.remove(oldName)
                appInfo.applyTemplates.add(newName)
            }
        }
        config.templates[newName] = config.templates[oldName]!!
        config.templates.remove(oldName)
        saveConfig()
    }

    fun updateTemplate(name: String, template: JsonConfig.Template) {
        Log.d(TAG, "updateTemplate: $name list = ${template.appList}")
        config.templates[name] = template
        saveConfig()
    }

    fun updateTemplateAppliedApps(name: String, appliedList: List<String>) {
        Log.d(TAG, "updateTemplateAppliedApps: $name list = $appliedList")
        config.scope.forEach { (app, appInfo) ->
            if (appliedList.contains(app)) appInfo.applyTemplates.add(name)
            else appInfo.applyTemplates.remove(name)
        }
        saveConfig()
    }

    fun isHideEnabled(packageName: String): Boolean {
        return config.scope.containsKey(packageName)
    }

    fun getAppConfig(packageName: String): JsonConfig.AppConfig? {
        return config.scope[packageName]
    }

    fun setAppConfig(packageName: String, appConfig: JsonConfig.AppConfig?) {
        if (appConfig == null) config.scope.remove(packageName)
        else config.scope[packageName] = appConfig
        saveConfig()
    }
}
