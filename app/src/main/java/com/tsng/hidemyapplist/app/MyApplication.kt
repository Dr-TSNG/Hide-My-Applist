package com.tsng.hidemyapplist.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.helpers.AppInfoHelper
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MyApplication : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
        val isModuleActivated = false
    }

    init {
        System.loadLibrary("natives")
    }

    private external fun nativeInit()

    private fun removeUninstalledApps() {
        JsonConfigManager.test()
        thread {
            val list = mutableSetOf<String>()
            AppInfoHelper.getAppInfoList().forEach { list.add(it.packageName) }
            JsonConfigManager.edit {
                templates.forEach { (_, template) ->
                    template.appList.removeIf {
                        !list.contains(it)
                    }
                }
                scope.entries.removeIf { !list.contains(it.key) }
            }
        }

    }

    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        nativeInit()
        if (!appContext.filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        removeUninstalledApps()
        startService(Intent(this, SubmitConfigService::class.java))
    }
}