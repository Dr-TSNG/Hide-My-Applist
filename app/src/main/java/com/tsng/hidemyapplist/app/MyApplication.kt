package com.tsng.hidemyapplist.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import com.tsng.hidemyapplist.R
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

    @SuppressLint("SdCardPath")
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        JsonConfigManager.test()
        if(!appContext.filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            exitProcess(0)
        }
        startService(Intent(this, SubmitConfigService::class.java))
    }
}