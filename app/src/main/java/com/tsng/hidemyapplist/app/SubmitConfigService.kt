package com.tsng.hidemyapplist.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.app.ServiceHelper.submitConfig
import kotlin.concurrent.thread

class SubmitConfigService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        thread {
            while (true) {
                //submitConfig(JsonConfigManager.toString())
                Thread.sleep(1000)
            }
        }
    }
}