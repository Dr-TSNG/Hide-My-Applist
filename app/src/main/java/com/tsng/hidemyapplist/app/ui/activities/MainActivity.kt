package com.tsng.hidemyapplist.app.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.Magic
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.MyApplication
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.SubmitConfigService
import com.tsng.hidemyapplist.app.helpers.ServiceHelper
import com.tsng.hidemyapplist.app.makeToast
import com.tsng.hidemyapplist.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val s = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
            s.apkContentsSigners[0].toByteArray()
        } else {
            val s = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            s[0].toByteArray()
        }
        if (sig.contentEquals(Magic.magicNumbers)) {
            startService(Intent(this, SubmitConfigService::class.java))
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        MobileAds.initialize(appContext)
        binding.adBanner.loadAd(AdRequest.Builder().build())
        makeUpdateAlert()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        val serviceVersion = ServiceHelper.getServiceVersion()
        if (MyApplication.isModuleActivated) {
            if (serviceVersion != 0) {
                binding.moduleStatusCard.setCardBackgroundColor(getColor(R.color.colorPrimary))
                binding.moduleStatusIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_activited))
                binding.moduleStatusText.text = getString(R.string.xposed_activated)
            } else {
                binding.moduleStatusCard.setCardBackgroundColor(getColor(R.color.service_off))
                binding.moduleStatusIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_service_not_running))
                binding.moduleStatusText.text = getString(R.string.xposed_activated)
            }
        } else {
            binding.moduleStatusCard.setCardBackgroundColor(getColor(R.color.gray))
            binding.moduleStatusIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_not_activated))
            binding.moduleStatusText.text = getString(R.string.xposed_not_activated)
        }
        if (serviceVersion != 0) {
            binding.serviceStatusText.text =
                if (serviceVersion != BuildConfig.SERVICE_VERSION)
                    getString(R.string.xposed_service_old)
                else
                    getString(R.string.xposed_service_on) + " [$serviceVersion]"
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
        binding.moduleStatusCard.setOnClickListener(this)
        binding.menuDetectionTest.setOnClickListener(this)
        binding.menuTemplateManage.setOnClickListener(this)
        binding.menuScopeManage.setOnClickListener(this)
        binding.menuLogs.setOnClickListener(this)
        binding.menuSettings.setOnClickListener(this)
        binding.menuAbout.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.module_status_card -> {
                if (ServiceHelper.getServiceVersion() == 0 || ServiceHelper.getRiruExtensionVersion() > 0) return
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_magisk_extension_title)
                    .setMessage(R.string.install_magisk_extension_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        val zipFile = File("$cacheDir/extension.zip")
                        assets.open("extension.zip").use { fis ->
                            zipFile.outputStream().use {
                                fis.copyTo(it)
                            }
                        }

                        val result = Shell.su("su --mount-master -c magisk --install-module ${zipFile.absolutePath}").exec()
                        dialog.dismiss()
                        val sb = StringBuilder()
                        result.out.forEach { sb.appendLine(it) }
                        result.err.forEach { sb.appendLine(it) }
                        sb.appendLine("----------")
                        sb.appendLine("Result code: ${result.code}")
                        val nd = MaterialAlertDialogBuilder(this).setMessage(sb)
                        if (result.isSuccess) {
                            nd.setTitle(R.string.install_magisk_extension_successful)
                            nd.setNegativeButton(android.R.string.cancel, null)
                            nd.setPositiveButton(R.string.reboot) { _, _ ->
                                Shell.su("reboot").exec()
                            }
                        } else {
                            nd.setTitle(R.string.install_magisk_extension_failed)
                            nd.setPositiveButton(android.R.string.ok, null)
                        }
                        nd.show()
                    }
                    .show()
            }
            R.id.menu_detection_test -> {
                val intent = packageManager.getLaunchIntentForPackage("com.tsng.applistdetector")
                if (intent == null) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.download_test_app_title)
                        .setMessage(R.string.download_test_app_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dr-TSNG/ApplistDetector/releases")))
                        }
                        .show()
                } else startActivity(intent)
            }
            R.id.menu_template_manage ->
                startActivity(Intent(this, ModuleActivity::class.java)
                    .putExtra("Fragment", ModuleActivity.Fragment.TEMPLATE_MANAGE))
            R.id.menu_scope_manage ->
                startActivity(Intent(this, ModuleActivity::class.java)
                    .putExtra("Fragment", ModuleActivity.Fragment.SCOPE_MANAGE))
            R.id.menu_logs ->
                if (ServiceHelper.getServiceVersion() == 0) makeToast(R.string.xposed_service_off)
                else startActivity(Intent(this, LogActivity::class.java))
            R.id.menu_settings -> startActivity(Intent(this, ModuleActivity::class.java)
                .putExtra("Fragment", ModuleActivity.Fragment.SETTINGS))
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun makeUpdateAlert() {
        if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("disableUpdate", false)) return
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val oldVersion = pref.getInt("lastVersion", 0)
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
                    if (getSharedPreferences("settings", MODE_PRIVATE).getBoolean("receiveBetaUpdate", false))
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
                    } else if (oldVersion < BuildConfig.VERSION_CODE) runOnUiThread {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.update_logs)
                            .setMessage(Html.fromHtml(updateLog, Html.FROM_HTML_MODE_COMPACT))
                            .setPositiveButton(android.R.string.ok, null)
                            .setCancelable(false)
                            .show()
                    }
                    pref.edit().putInt("lastVersion", BuildConfig.VERSION_CODE).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}