package com.tsng.hidemyapplist.app.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.helpers.ServiceHelper
import com.tsng.hidemyapplist.app.SubmitConfigService
import com.tsng.hidemyapplist.app.makeToast
import com.tsng.hidemyapplist.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        val isModuleActivated = false
    }

    init {
        System.loadLibrary("natives")
    }

    private lateinit var binding: ActivityMainBinding

    private fun isHookSelf(): Boolean {
        return getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("HookSelf", false)
    }

    @SuppressLint("SdCardPath")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        if(!filesDir.absolutePath.startsWith("/data/user/0/")) {
            makeToast(R.string.do_not_dual)
            finish()
            exitProcess(0)
        }
        startService(Intent(this, SubmitConfigService::class.java))
        makeUpdateAlert()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        val serviceVersion = ServiceHelper.getServiceVersion()
        if (isModuleActivated) {
            if (serviceVersion != 0) {
                binding.moduleStatusCard.setCardBackgroundColor(getColor(R.color.colorPrimary))
                binding.moduleStatusIcon.setImageDrawable(getDrawable(R.drawable.ic_activited))
                binding.moduleStatusText.text = getString(R.string.xposed_activated)
            } else {
                binding.moduleStatusCard.setCardBackgroundColor(getColor(R.color.service_off))
                binding.moduleStatusIcon.setImageDrawable(getDrawable(R.drawable.ic_service_not_running))
                binding.moduleStatusText.text = getString(R.string.xposed_activated)
            }
        } else {
            binding.moduleStatusCard.setCardBackgroundColor(getColor(R.color.gray))
            binding.moduleStatusIcon.setImageDrawable(getDrawable(R.drawable.ic_not_activated))
            binding.moduleStatusText.text = getString(R.string.xposed_not_activated)
        }
        if (serviceVersion != 0) {
            if (serviceVersion != BuildConfig.SERVICE_VERSION) binding.serviceStatusText.text = getString(
                R.string.xposed_service_old
            )
            else binding.serviceStatusText.text = getString(R.string.xposed_service_on) + " [$serviceVersion]"
            val text = getString(R.string.xposed_serve_times).split("#")
            binding.serveTimes.visibility = View.VISIBLE
            binding.serveTimes.text = text[0] + ServiceHelper.getServeTimes() + text[2]
            binding.riruStatusText.visibility = View.VISIBLE
            binding.riruStatusText.text = when (val riruExtensionVersion =
                ServiceHelper.getRiruExtensionVersion()) {
                0 -> getString(R.string.riru_not_installed)
                -1 -> getString(R.string.riru_version_too_old)
                -2 -> getString(R.string.riru_apk_version_too_old)
                else -> getString(R.string.riru_installed) + " [$riruExtensionVersion]"
            }
        } else {
            binding.serveTimes.visibility = View.GONE
            binding.serviceStatusText.text = getString(R.string.xposed_service_off)
        }
        binding.menuDetectionTest.setOnClickListener(this)
        binding.menuTemplateManage.setOnClickListener(this)
        binding.menuScopeManage.setOnClickListener(this)
        binding.menuLogs.setOnClickListener(this)
        binding.menuSettings.setOnClickListener(this)
        binding.menuAbout.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.menu_detection_test -> startActivity(Intent(this, DetectionActivity::class.java))
            R.id.menu_template_manage ->
                if (isHookSelf()) makeToast(R.string.xposed_disable_hook_self_first)
                else startActivity(Intent(this, TemplateManageActivity::class.java))
            R.id.menu_scope_manage ->
                if (isHookSelf()) makeToast(R.string.xposed_disable_hook_self_first)
                else startActivity(Intent(this, ScopeManageActivity::class.java))
            R.id.menu_logs ->
                if (ServiceHelper.getServiceVersion() == 0) makeToast(R.string.xposed_service_off)
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
                        .url("https://cdn.jsdelivr.net/gh/Dr-TSNG/Hide-My-Applist@updates/updates/latest_version.json")
                        .build()).execute().body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    var data = json["Stable"] as JSONObject
                    var updateLogURL = "https://cdn.jsdelivr.net/gh/Dr-TSNG/Hide-My-Applist@updates/updates/stable-"
                    if (getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("ReceiveBetaUpdate", false))
                        if (json["Beta"] != false) {
                            data = json["Beta"] as JSONObject
                            updateLogURL = "https://cdn.jsdelivr.net/gh/Dr-TSNG/Hide-My-Applist@updates/updates/beta-"
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
                                .setNegativeButton("TG Channel") { _, _ ->
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/HideMyApplist")))
                                }
                                .setNeutralButton(android.R.string.cancel, null)
                                .setCancelable(false).show()
                    } else if (pref.getInt("LastVersion", 0) < BuildConfig.VERSION_CODE) runOnUiThread {
                        MaterialAlertDialogBuilder(this).setTitle(R.string.update_logs)
                                .setMessage(Html.fromHtml(updateLog, Html.FROM_HTML_MODE_COMPACT))
                                .setPositiveButton(android.R.string.ok, null)
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