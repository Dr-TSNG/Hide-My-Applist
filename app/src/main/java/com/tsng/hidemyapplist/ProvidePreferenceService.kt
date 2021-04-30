package com.tsng.hidemyapplist

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.gson.Gson
import kotlin.concurrent.thread

class ProvidePreferenceService : Service() {

    override fun onBind(intent: Intent): IBinder? { return null }

    override fun onCreate() {
        thread {
            while (true) {
                val json = JsonConfig()
                json.HookSelf = getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("HookSelf", false)
                json.DetailLog = getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("DetailLog", false)
                json.Scope = getSharedPreferences("Scope", MODE_PRIVATE).all as Map<String, String>
                for (template in getSharedPreferences("Templates", MODE_PRIVATE).getStringSet("List", setOf())) {
                    val obj = getSharedPreferences("tpl_$template", MODE_PRIVATE).all
                    json.Templates[template] = Gson().fromJson(Gson().toJson(obj), JsonConfig.Template::class.java)
                }
                try {
                    packageManager.getInstallerPackageName("providePreference#$json")
                } catch (e: IllegalArgumentException) { }
                Thread.sleep(1000)
            }
        }
    }
}