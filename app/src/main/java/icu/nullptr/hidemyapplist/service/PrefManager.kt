package icu.nullptr.hidemyapplist.service

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import icu.nullptr.hidemyapplist.hmaApp

object PrefManager {

    private const val PREF_HIDE_ICON = "hide_icon"
    private const val PREF_DISABLE_UPDATE = "disable_update"
    private const val PREF_RECEIVE_BETA_UPDATE = "receive_beta_update"
    private const val PREF_APP_FILTER_SHOW_SYSTEM = "app_filter_show_system"
    private const val PREF_APP_FILTER_SORT_METHOD = "app_filter_sort_method"
    private const val PREF_APP_FILTER_REVERSE_ORDER = "app_filter_reverse_order"
    private const val PREF_LOG_FILTER_LEVEL = "log_filter_level"
    private const val PREF_LOG_FILTER_REVERSE_ORDER = "log_filter_reverse_order"

    enum class SortMethod {
        BY_LABEL, BY_PACKAGE_NAME, BY_INSTALL_TIME, BY_UPDATE_TIME
    }

    private val pref = hmaApp.getSharedPreferences("settings", MODE_PRIVATE)

    var hideIcon: Boolean
        get() = pref.getBoolean(PREF_HIDE_ICON, false)
        set(value) {
            pref.edit().putBoolean(PREF_HIDE_ICON, value).apply()
            val component = ComponentName(hmaApp, "com.tsng.hidemyapplist.MainActivityLauncher")
            val status =
                if (value) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            hmaApp.packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
        }

    var disableUpdate: Boolean
        get() = pref.getBoolean(PREF_DISABLE_UPDATE, false)
        set(value) = pref.edit().putBoolean(PREF_DISABLE_UPDATE, value).apply()

    var receiveBetaUpdate: Boolean
        get() = pref.getBoolean(PREF_RECEIVE_BETA_UPDATE, false)
        set(value) = pref.edit().putBoolean(PREF_RECEIVE_BETA_UPDATE, value).apply()

    var appFilter_showSystem: Boolean
        get() = pref.getBoolean(PREF_APP_FILTER_SHOW_SYSTEM, false)
        set(value) = pref.edit().putBoolean(PREF_APP_FILTER_SHOW_SYSTEM, value).apply()

    var appFilter_sortMethod: SortMethod
        get() = SortMethod.values()[pref.getInt(PREF_APP_FILTER_SORT_METHOD, SortMethod.BY_LABEL.ordinal)]
        set(value) = pref.edit().putInt(PREF_APP_FILTER_SORT_METHOD, value.ordinal).apply()

    var appFilter_reverseOrder: Boolean
        get() = pref.getBoolean(PREF_APP_FILTER_REVERSE_ORDER, false)
        set(value) = pref.edit().putBoolean(PREF_APP_FILTER_REVERSE_ORDER, value).apply()

    var logFilter_level: Int
        get() = pref.getInt(PREF_LOG_FILTER_LEVEL, 0)
        set(value) = pref.edit().putInt(PREF_LOG_FILTER_LEVEL, value).apply()

    var logFilter_reverseOrder: Boolean
        get() = pref.getBoolean(PREF_LOG_FILTER_REVERSE_ORDER, false)
        set(value) = pref.edit().putBoolean(PREF_LOG_FILTER_REVERSE_ORDER, value).apply()
}
