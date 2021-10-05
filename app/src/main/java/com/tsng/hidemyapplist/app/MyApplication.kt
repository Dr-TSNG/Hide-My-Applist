package com.tsng.hidemyapplist.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.tsng.hidemyapplist.BuildConfig
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

    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        nativeInit()
        if (!appContext.filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        JsonConfigManager.edit {
            scope.remove(BuildConfig.APPLICATION_ID)
        }
        thread {
            AppInfoHelper.getAppInfoList()
        }
    }
}