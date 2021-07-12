package com.tsng.hidemyapplist.app.helpers

import androidx.preference.PreferenceDataStore
import com.github.kyuubiran.ezxhelper.utils.getFieldByClassOrObject
import com.tsng.hidemyapplist.JsonConfig.AppConfig
import com.tsng.hidemyapplist.JsonConfig.Template

class TemplateDataStorage(private val template: Template) : PreferenceDataStore() {

}

class AppConfigDataStorage(private val appConfig: AppConfig) : PreferenceDataStore() {
    var isEnabled = false

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return if (key == "isEnabled") isEnabled
        else AppConfig::class.java.getFieldByClassOrObject(key).getBoolean(appConfig)
    }

    override fun putBoolean(key: String, value: Boolean) {
        if (key == "isEnabled") isEnabled = value
        else AppConfig::class.java.getFieldByClassOrObject(key).setBoolean(appConfig, value)
    }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String> {
        return AppConfig::class.java.getFieldByClassOrObject(key)
            .get(appConfig) as MutableSet<String>
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) {
        AppConfig::class.java.getFieldByClassOrObject(key).set(appConfig, values)
    }
}