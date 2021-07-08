package com.tsng.hidemyapplist.app.ui.activities

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.helpers.ServiceHelper
import com.tsng.hidemyapplist.app.makeToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_container, SettingsFragment())
                    .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val backupImportSAFLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            val tmpDir = "${appContext.cacheDir.path}/backup"
            try {
                val tmpFile = "$tmpDir/backup.tar.gz"
                File(tmpDir).mkdirs()
                appContext.contentResolver.openInputStream(uri).use { fis ->
                    if (fis == null) throw RuntimeException(getString(R.string.settings_import_file_damaged))
                    File(tmpFile).outputStream().use { fos ->
                        fis.copyTo(fos)
                    }
                }
                Runtime.getRuntime().exec("tar -xzf $tmpFile -C $tmpDir").waitFor()
                val backupVersion: Int
                try {
                    backupVersion = File("$tmpDir/version").readText().toInt()
                } catch (e: Exception) {
                    throw RuntimeException(getString(R.string.settings_import_file_damaged))
                        .apply { addSuppressed(e) }
                }
                if (backupVersion > BuildConfig.VERSION_CODE) throw RuntimeException(getString(R.string.settings_import_app_version_too_old))
                if (backupVersion < BuildConfig.MIN_BACKUP_VERSION) throw RuntimeException(getString(R.string.settings_import_backup_version_too_old))
                File("${appContext.dataDir.path}/shared_prefs").deleteRecursively()
                Runtime.getRuntime().exec("mv $tmpDir/shared_prefs ${appContext.dataDir.path}").waitFor()
                Thread.sleep(50)
                // TODO: Stub, 设置页的没刷新
                makeToast(R.string.settings_import_successful)
            } catch (e: Throwable) {
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
            } finally {
                File(tmpDir).deleteRecursively()
            }
        }

        private val backupExportSAFLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            appContext.cacheDir.mkdir()
            val tmpFile = "${appContext.cacheDir.path}/backup.tar.gz"
            File(tmpFile).delete()
            File("${appContext.dataDir.path}/version").writeText(BuildConfig.VERSION_CODE.toString())
            Runtime.getRuntime().exec("tar -czf $tmpFile -C ${appContext.dataDir.path} version shared_prefs").waitFor()
            Thread.sleep(50)
            File(tmpFile).inputStream().use { fis ->
                appContext.contentResolver.openOutputStream(uri).use {  fos ->
                    if (fos == null) makeToast(R.string.settings_export_failed)
                    else fis.copyTo(fos)
                }
            }
            File("${appContext.dataDir.path}/version").delete()
            File(tmpFile).delete()
            makeToast(R.string.settings_exported)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "Settings"
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            module()
            service()
            backup()
        }

        private fun module() {
            preferenceScreen.findPreference<SwitchPreferenceCompat>("HookSelf")?.apply {
                setOnPreferenceClickListener {
                    makeToast(R.string.settings_hook_self_toast)
                    true
                }
            }
            preferenceScreen.findPreference<ListPreference>("MaxLogSize")?.apply {
                summary = entry
                setOnPreferenceChangeListener { preference, newValue ->
                    summary = (preference as ListPreference).run { entries[findIndexOfValue(newValue as String)] }
                    true
                }
            }
            preferenceScreen.findPreference<SwitchPreferenceCompat>("HideIcon")?.setOnPreferenceChangeListener { _, newValue ->
                val component = ComponentName(appContext, "com.tsng.hidemyapplist.MainActivityLauncher")
                val status = if (newValue == true) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                appContext.packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
                true
            }
        }

        private fun service() {
            preferenceScreen.findPreference<Preference>("StopSystemService")?.setOnPreferenceClickListener {
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
            preferenceScreen.findPreference<Preference>("ForceCleanEnv")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.settings_force_clean_env)
                    .setMessage(R.string.settings_is_clean_env_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val result = Shell.su("rm -rf /data/misc/hide_my_applist /data/misc/hma_selinux_test").exec().isSuccess
                        if (result) makeToast(R.string.settings_force_clean_env_toast_successful)
                        else makeToast(R.string.settings_force_clean_env_toast_failed)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
        }

        private fun backup() {
            preferenceScreen.findPreference<Preference>("ImportConfig")?.setOnPreferenceClickListener {
                backupImportSAFLauncher.launch("*/*")
                true
            }
            preferenceScreen.findPreference<Preference>("ExportConfig")?.setOnPreferenceClickListener {
                val date = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                backupExportSAFLauncher.launch("$date.hmaconf")
                true
            }
        }
    }
}