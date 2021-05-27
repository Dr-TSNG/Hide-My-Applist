package com.tsng.hidemyapplist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.ui.*
import com.tsng.hidemyapplist.xposed.XposedUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        var riruModuleVersion = 0
    }

    init {
        System.loadLibrary("natives")
        if (riruModuleVersion == 0) riruModuleVersion = getRiruModuleVersion()
    }

    private fun isModuleActivated(): Boolean {
        return false
    }

    private external fun getRiruModuleVersion(): Int

    private fun isHookSelf(): Boolean {
        return getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("HookSelf", false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        startService(Intent(this, ProvidePreferenceService::class.java))
        makeUpdateAlert()
    }

    override fun onResume() {
        super.onResume()
        val serviceVersion = XposedUtils.getServiceVersion(this)
        if (isModuleActivated()) {
            if (serviceVersion != 0) {
                xposed_status.setCardBackgroundColor(getColor(R.color.colorPrimary))
                xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_activited))
                xposed_status_text.text = getString(R.string.xposed_activated)
            } else {
                xposed_status.setCardBackgroundColor(getColor(R.color.service_off))
                xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_service_not_running))
                xposed_status_text.text = getString(R.string.xposed_activated)
            }
        } else {
            xposed_status.setCardBackgroundColor(getColor(R.color.gray))
            xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_not_activated))
            xposed_status_text.text = getString(R.string.xposed_not_activated)
        }
        if (serviceVersion != 0) {
            if (serviceVersion != BuildConfig.SERVICE_VERSION) xposed_status_sub_text.text = getString(R.string.xposed_service_old)
            else xposed_status_sub_text.text = getString(R.string.xposed_service_on) + "$serviceVersion]"
            val text = getString(R.string.xposed_serve_times).split("#")
            xposed_status_serve_times.visibility = View.VISIBLE
            xposed_status_serve_times.text = text[0] + XposedUtils.getServeTimes(this) + text[2]
            riru_status_text.visibility = View.VISIBLE
            if (riruModuleVersion == 0)
                riru_status_text.text = getString(R.string.riru_status) + getString(R.string.riru_not_installed)
            else
                riru_status_text.text = getString(R.string.riru_status) + getString(R.string.riru_installed) + "$riruModuleVersion]"
        } else {
            xposed_status_serve_times.visibility = View.GONE
            xposed_status_sub_text.text = getString(R.string.xposed_service_off)
        }
        menu_detection_test.setOnClickListener(this)
        menu_template_manage.setOnClickListener(this)
        menu_scope_manage.setOnClickListener(this)
        menu_logs.setOnClickListener(this)
        menu_settings.setOnClickListener(this)
        menu_about.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.menu_detection_test -> startActivity(Intent(this, DetectionActivity::class.java))
            R.id.menu_template_manage ->
                if (isHookSelf()) Toast.makeText(this, R.string.xposed_disable_hook_self_first, Toast.LENGTH_SHORT).show()
                else startActivity(Intent(this, TemplateManageActivity::class.java))
            R.id.menu_scope_manage ->
                if (isHookSelf()) Toast.makeText(this, R.string.xposed_disable_hook_self_first, Toast.LENGTH_SHORT).show()
                else startActivity(Intent(this, ScopeManageActivity::class.java))
            R.id.menu_logs ->
                if (XposedUtils.getServiceVersion(this) == 0)
                    Toast.makeText(this, R.string.xposed_service_off, Toast.LENGTH_SHORT).show()
                else startActivity(Intent(this, LogActivity::class.java))
            R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun makeUpdateAlert() {
        if (getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("DisableUpdate", false)) return;
        thread {
            try {
                val client = OkHttpClient()
                val responseData = client.newCall(Request.Builder()
                        .url("https://cdn.jsdelivr.net/gh/Dr-TSNG/Hide-My-Applist@master/updates/latest_version.json")
                        .build()).execute().body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    var data = json["Stable"] as JSONObject
                    var updateLogURL = "https://cdn.jsdelivr.net/gh/Dr-TSNG/Hide-My-Applist@master/updates/stable-"
                    if (getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("ReceiveBetaUpdate", false))
                        if (json["Beta"] != false) {
                            data = json["Beta"] as JSONObject
                            updateLogURL = "https://cdn.jsdelivr.net/gh/Dr-TSNG/Hide-My-Applist@master/updates/beta-"
                        }
                    updateLogURL += if (Locale.getDefault().language.contains("zh")) "zh" else "en"
                    updateLogURL += ".html"
                    val updateLog = client.newCall(Request.Builder()
                            .url(updateLogURL)
                            .build()).execute().body?.string()
                    val githubDownloadUri = Uri.parse(data["DownloadURL"] as String)
                    val pref = PreferenceManager.getDefaultSharedPreferences(this)
                    if (data.getInt("VersionCode") > BuildConfig.VERSION_CODE) runOnUiThread {
                        MaterialAlertDialogBuilder(this)
                                .setTitle(getString(R.string.new_update) + data["VersionName"])
                                .setMessage(Html.fromHtml(updateLog, Html.FROM_HTML_MODE_COMPACT))
                                .setPositiveButton("GitHub") { _, _ ->
                                    startActivity(Intent(Intent.ACTION_VIEW, githubDownloadUri))
                                }
                                .setNegativeButton("XP Repo") { _, _ ->
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://repo.xposed.info/module/com.tsng.hidemyapplist")))
                                }
                                .setNeutralButton(R.string.cancel, null)
                                .setCancelable(false).show()
                    } else if (pref.getInt("LastVersion", 0) < BuildConfig.VERSION_CODE) runOnUiThread {
                        MaterialAlertDialogBuilder(this).setTitle(R.string.update_logs)
                                .setMessage(Html.fromHtml(updateLog, Html.FROM_HTML_MODE_COMPACT))
                                .setPositiveButton(R.string.accept, null)
                                .setCancelable(false).show()
                    }
                    pref.edit().putInt("LastVersion", BuildConfig.VERSION_CODE).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}