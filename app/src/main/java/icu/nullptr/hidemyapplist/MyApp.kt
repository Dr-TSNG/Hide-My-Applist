package icu.nullptr.hidemyapplist

import android.annotation.SuppressLint
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.receiver.AppChangeReceiver
import icu.nullptr.hidemyapplist.ui.util.makeToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.zhanghai.android.appiconloader.AppIconLoader
import rikka.material.app.LocaleDelegate
import java.util.*
import kotlin.system.exitProcess

lateinit var hmaApp: MyApp

class MyApp : Application() {

    @JvmField
    var isHooked = false

    val globalScope = CoroutineScope(Dispatchers.Default)
    val appIconLoader by lazy {
        val iconSize = resources.getDimensionPixelSize(R.dimen.app_icon_size)
        AppIconLoader(iconSize, false, this)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        if (!filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        hmaApp = this
        AppChangeReceiver.register(this)
        ConfigManager.init()

        AppCompatDelegate.setDefaultNightMode(PrefManager.darkTheme)
        LocaleDelegate.defaultLocale = getLocale(PrefManager.locale)
        val config = resources.configuration
        config.setLocale(LocaleDelegate.defaultLocale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun getLocale(tag: String): Locale {
        return if (tag == "SYSTEM") LocaleDelegate.systemLocale
        else Locale.forLanguageTag(tag)
    }
}
