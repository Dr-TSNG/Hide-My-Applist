package com.tsng.hidemyapplist

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import kotlin.concurrent.thread

class ProvidePreferenceService : Service() {
    companion object {
        @JvmStatic
        fun sendConfig(context: Context) {
            val json = JsonConfig()
            json.HookSelf = context.getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("HookSelf", false)
            json.DetailLog = context.getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("DetailLog", false)
            json.MaxLogSize = context.getSharedPreferences("Settings", MODE_PRIVATE).getString("MaxLogSize", "512").toInt()
            json.Scope = context.getSharedPreferences("Scope", MODE_PRIVATE).all as Map<String, String>
            for (template in context.getSharedPreferences("Templates", MODE_PRIVATE).getStringSet("List", setOf())) {
                val obj = context.getSharedPreferences("tpl_$template", MODE_PRIVATE).all
                json.Templates[template] = Gson().fromJson(Gson().toJson(obj), JsonConfig.Template::class.java)
            }
            try {
                context.packageManager.getInstallerPackageName("providePreference#$json")
            } catch (e: IllegalArgumentException) { }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        thread {
            while (true) {
                sendConfig(this)
                Thread.sleep(1000)
            }
        }
    }
}