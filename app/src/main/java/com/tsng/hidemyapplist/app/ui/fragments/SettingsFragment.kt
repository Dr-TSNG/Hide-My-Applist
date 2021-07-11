package com.tsng.hidemyapplist.app.ui.fragments

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.topjohnwu.superuser.Shell
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.helpers.ServiceHelper
import com.tsng.hidemyapplist.app.makeToast
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {
    private val backupImportSAFLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                val backup = appContext.contentResolver
                    .openInputStream(uri)?.reader().use { it?.readText() }
                    ?: throw RuntimeException(getString(R.string.settings_import_file_damaged))
                val backupJson: JsonObject
                val backupVersion: Int
                try {
                    backupJson = JsonParser.parseString(backup).asJsonObject
                    backupVersion = backupJson["configVersion"].asInt
                } catch (e: Exception) {
                    throw RuntimeException(getString(R.string.settings_import_file_damaged))
                        .apply { addSuppressed(e) }
                }
                if (backupVersion > BuildConfig.VERSION_CODE)
                    throw RuntimeException(getString(R.string.settings_import_app_version_too_old))
                if (backupVersion < BuildConfig.MIN_BACKUP_VERSION)
                    throw RuntimeException(getString(R.string.settings_import_backup_version_too_old))
                JsonConfigManager.edit {
                    templates.clear()
                    for ((name, template) in backupJson["templates"].asJsonObject.entrySet())
                        templates[name] = Gson().fromJson(template.toString(), JsonConfig.Template::class.java)
                    scope.clear()
                    for ((name, appConfig) in backupJson["scope"].asJsonObject.entrySet())
                        scope[name] = Gson().fromJson(appConfig.toString(), JsonConfig.AppConfig::class.java)
                }
                makeToast(R.string.settings_import_successful)
            } catch (e: Exception) {
                e.printStackTrace()
                MaterialAlertDialogBuilder(requireActivity())
                    .setCancelable(false)
                    .setTitle(R.string.settings_import_failed)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.show_crash_log) { _, _ ->
                        MaterialAlertDialogBuilder(requireActivity())
                            .setCancelable(false)
                            .setTitle(R.string.settings_import_failed)
                            .setMessage(e.stackTraceToString())
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }.show()
            }
        }

    private val backupExportSAFLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            JsonConfigManager.configFile.inputStream().use { fis ->
                appContext.contentResolver.openOutputStream(uri).use { fos ->
                    if (fos == null) makeToast(R.string.settings_export_failed)
                    else fis.copyTo(fos)
                }
            }
            makeToast(R.string.settings_exported)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "settings"
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        module()
        service()
        backup()
    }

    private fun module() {
        preferenceScreen.findPreference<SwitchPreferenceCompat>("hookSelf")
            ?.setOnPreferenceChangeListener { _, newValue ->
                JsonConfigManager.edit { hookSelf = newValue as Boolean }
                makeToast(R.string.settings_hook_self_toast)
                true
            }

        preferenceScreen.findPreference<SwitchPreferenceCompat>("detailLog")
            ?.setOnPreferenceChangeListener { _, newValue ->
                JsonConfigManager.edit { detailLog = newValue as Boolean }
                true
            }

        preferenceScreen.findPreference<ListPreference>("maxLogSize")?.apply {
            summary = entry
            setOnPreferenceChangeListener { _, newValue ->
                JsonConfigManager.edit {
                    maxLogSize = (newValue as String).toInt()
                }
                summary = entries[findIndexOfValue(newValue as String)]
                true
            }
        }

        preferenceScreen.findPreference<SwitchPreferenceCompat>("hideIcon")
            ?.setOnPreferenceChangeListener { _, newValue ->
                val component =
                    ComponentName(appContext, "com.tsng.hidemyapplist.MainActivityLauncher")
                val status =
                    if (newValue == true) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                appContext.packageManager
                    .setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
                true
            }
    }

    private fun service() {
        preferenceScreen.findPreference<Preference>("stopSystemService")
            ?.setOnPreferenceClickListener {
                if (ServiceHelper.getServiceVersion() != 0) {
                    MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.settings_is_clean_env)
                        .setMessage(R.string.settings_is_clean_env_summary)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            ServiceHelper.stopSystemService(true)
                            makeToast(R.string.settings_stop_system_service)
                        }
                        .setNegativeButton(R.string.no) { _, _ ->
                            ServiceHelper.stopSystemService(false)
                            makeToast(R.string.settings_stop_system_service)
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                } else makeToast(R.string.xposed_service_off)
                true
            }
        preferenceScreen.findPreference<Preference>("forceCleanEnv")
            ?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.settings_force_clean_env)
                    .setMessage(R.string.settings_is_clean_env_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val result =
                            Shell.su("rm -rf /data/misc/hide_my_applist /data/misc/hma_selinux_test")
                                .exec().isSuccess
                        if (result) makeToast(R.string.settings_force_clean_env_toast_successful)
                        else makeToast(R.string.settings_force_clean_env_toast_failed)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
    }

    private fun backup() {
        preferenceScreen.findPreference<Preference>("importConfig")
            ?.setOnPreferenceClickListener {
                backupImportSAFLauncher.launch("application/json")
                true
            }
        preferenceScreen.findPreference<Preference>("exportConfig")
            ?.setOnPreferenceClickListener {
                backupExportSAFLauncher.launch("HMA Config.json")
                true
            }
    }
}