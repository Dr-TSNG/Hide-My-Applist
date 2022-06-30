package com.tsng.hidemyapplist.app.helpers

import androidx.preference.PreferenceDataStore
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import icu.nullptr.hidemyapplist.common.JsonConfig

class AppConfigDataStorage(
    private val packageName: String,
    private val appConfig: JsonConfig.AppConfig
) : PreferenceDataStore() {
    var isEnabled = globalConfig.scope.containsKey(packageName)
        set(value) {
            field = value
            if (value) {
                globalConfig.scope[packageName] = appConfig
            } else {
                globalConfig.scope.remove(packageName)
            }
        }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return if (key == "isEnabled") isEnabled
        else JsonConfig.AppConfig::class.java.getDeclaredField(key)
            .also { it.isAccessible = true }
            .getBoolean(appConfig)
    }

    override fun putBoolean(key: String, value: Boolean) {
        if (key == "isEnabled") isEnabled = value
        else JsonConfig.AppConfig::class.java.getDeclaredField(key)
            .also { it.isAccessible = true }
            .setBoolean(appConfig, value)
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String> {
        return JsonConfig.AppConfig::class.java.getDeclaredField(key)
            .also { it.isAccessible = true }
            .get(appConfig) as MutableSet<String>
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        JsonConfig.AppConfig::class.java.getDeclaredField(key)
            .also { it.isAccessible = true }
            .set(appConfig, values)
    }
}
