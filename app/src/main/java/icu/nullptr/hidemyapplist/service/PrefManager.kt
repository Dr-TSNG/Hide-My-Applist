package icu.nullptr.hidemyapplist.service

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import icu.nullptr.hidemyapplist.hmaApp

object PrefManager {

    private const val PREF_LAST_VERSION = "last_version"

    private const val PREF_LOCALE = "language"

    private const val PREF_DARK_THEME = "dark_theme"
    private const val PREF_BLACK_DARK_THEME = "black_dark_theme"
    private const val PREF_FOLLOW_SYSTEM_ACCENT = "follow_system_accent"
    private const val PREF_THEME_COLOR = "theme_color"

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

    var lastVersion: Int
        get() = pref.getInt(PREF_LAST_VERSION, 0)
        set(value) = pref.edit().putInt(PREF_LAST_VERSION, value).apply()

    var locale: String
        get() = pref.getString(PREF_LOCALE, "SYSTEM")!!
        set(value) = pref.edit().putString(PREF_LOCALE, value).apply()

    var darkTheme: Int
        get() = pref.getInt(PREF_DARK_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = pref.edit().putInt(PREF_DARK_THEME, value).apply()

    var blackDarkTheme: Boolean
        get() = pref.getBoolean(PREF_BLACK_DARK_THEME, false)
        set(value) = pref.edit().putBoolean(PREF_BLACK_DARK_THEME, value).apply()

    var followSystemAccent: Boolean
        get() = pref.getBoolean(PREF_FOLLOW_SYSTEM_ACCENT, true)
        set(value) = pref.edit().putBoolean(PREF_FOLLOW_SYSTEM_ACCENT, value).apply()

    var themeColor: String
        get() = pref.getString(PREF_THEME_COLOR, "MATERIAL_BLUE")!!
        set(value) = pref.edit().putString(PREF_THEME_COLOR, value).apply()

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
