package icu.nullptr.hidemyapplist

import android.app.Application
import android.content.SharedPreferences
import com.google.android.material.color.DynamicColors
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.ui.util.makeToast
import kotlin.system.exitProcess

lateinit var hmaApp: MyApp

class MyApp : Application() {

    @JvmField
    var isHooked = false

    lateinit var pref: SharedPreferences

    override fun onCreate() {
        if (!filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        hmaApp = this
        pref = getSharedPreferences("settings", MODE_PRIVATE)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
