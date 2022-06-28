package com.tsng.hidemyapplist.app.helpers

import androidx.preference.PreferenceDataStore
import icu.nullptr.hidemyapplist.common.JsonConfig

class AppConfigDataStorage(private val appConfig: JsonConfig.AppConfig) : PreferenceDataStore() {
    var isEnabled = false

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return if (key == "isEnabled") isEnabled
        else JsonConfig.AppConfig::class.java.getField(key).getBoolean(appConfig)
    }

    override fun putBoolean(key: String, value: Boolean) {
        if (key == "isEnabled") isEnabled = value
        else JsonConfig.AppConfig::class.java.getField(key).setBoolean(appConfig, value)
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String> {
        return JsonConfig.AppConfig::class.java.getField(key)
            .get(appConfig) as MutableSet<String>
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        JsonConfig.AppConfig::class.java.getField(key).set(appConfig, values)
    }
}
