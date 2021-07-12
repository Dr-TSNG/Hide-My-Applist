package com.tsng.hidemyapplist.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.helpers.ServiceHelper
import kotlin.concurrent.thread

class SubmitConfigService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        thread {
            while (true) {
                ServiceHelper.submitConfig(globalConfig.toString())
                Thread.sleep(1000)
            }
        }
    }
}