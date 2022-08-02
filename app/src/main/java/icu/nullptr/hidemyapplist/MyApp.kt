package icu.nullptr.hidemyapplist

import android.app.Application
import android.content.SharedPreferences
import coil.Coil
import coil.ImageLoader
import coil.fetch.Fetcher
import com.google.android.material.color.DynamicColors
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.ui.util.makeToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.zhanghai.android.appiconloader.coil.AppIconFetcher
import kotlin.system.exitProcess

lateinit var hmaApp: MyApp

class MyApp : Application() {

    @JvmField
    var isHooked = false

    val globalScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        if (!filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        hmaApp = this
        DynamicColors.applyToActivitiesIfAvailable(this)
        val iconSize = hmaApp.resources.getDimensionPixelSize(R.dimen.app_icon_size)
        val imageLoader = ImageLoader.Builder(hmaApp)
            .components {
                add(Fetcher.Factory { _, _, _ ->
                    AppIconFetcher(iconSize, false, hmaApp)
                })
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
