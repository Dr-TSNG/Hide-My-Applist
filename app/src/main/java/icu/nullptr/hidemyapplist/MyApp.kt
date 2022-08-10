package icu.nullptr.hidemyapplist

import android.annotation.SuppressLint
import android.app.Application
import com.google.android.material.color.DynamicColors
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.ui.util.makeToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.zhanghai.android.appiconloader.AppIconLoader
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

    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        if (!filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        hmaApp = this
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
