package com.tsng.hidemyapplist.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
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
import com.tsng.hidemyapplist.xposed.XposedUtils
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
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
        lateinit var activity: SettingsActivity

        private val backupImportSAFLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            val tmpDir = "${activity.cacheDir.path}/backup"
            try {
                val tmpFile = "$tmpDir/backup.tar.gz"
                File(tmpDir).mkdirs()
                activity.contentResolver.openInputStream(uri).use { fis ->
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
                File("${activity.dataDir.path}/shared_prefs").deleteRecursively()
                Runtime.getRuntime().exec("mv $tmpDir/shared_prefs ${activity.dataDir.path}").waitFor()
                Thread.sleep(50)
                // TODO: Stub, 设置页的没刷新
                Toast.makeText(activity, R.string.settings_import_successful, Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                e.printStackTrace()
                MaterialAlertDialogBuilder(activity)
                    .setCancelable(false)
                    .setTitle(R.string.settings_import_failed)
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.show_crash_log) { _, _ ->
                        MaterialAlertDialogBuilder(activity)
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
            activity.cacheDir.mkdir()
            val tmpFile = "${activity.cacheDir.path}/backup.tar.gz"
            File(tmpFile).delete()
            File("${activity.dataDir.path}/version").writeText(BuildConfig.VERSION_CODE.toString())
            Runtime.getRuntime().exec("tar -czf $tmpFile -C ${activity.dataDir.path} version shared_prefs").waitFor()
            Thread.sleep(50)
            File(tmpFile).inputStream().use { fis ->
                activity.contentResolver.openOutputStream(uri).use {  fos ->
                    fis.copyTo(fos)
                }
            }
            File("${activity.dataDir.path}/version").delete()
            File(tmpFile).delete()
            Toast.makeText(activity, R.string.settings_exported, Toast.LENGTH_SHORT).show()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            activity = requireActivity() as SettingsActivity
            preferenceManager.sharedPreferencesName = "Settings"
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            module()
            service()
            backup()
        }

        private fun module() {
            preferenceScreen.findPreference<SwitchPreferenceCompat>("HookSelf")?.apply {
                setOnPreferenceClickListener {
                    Toast.makeText(activity, R.string.settings_hook_self_toast, Toast.LENGTH_SHORT).show()
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
                val component = ComponentName(activity, "com.tsng.hidemyapplist.MainActivityLauncher")
                val status = if (newValue == true) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                activity.packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
                true
            }
        }

        private fun service() {
            preferenceScreen.findPreference<Preference>("StopSystemService")?.setOnPreferenceClickListener {
                if (XposedUtils.getServiceVersion(activity) != 0) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.settings_is_clean_env)
                        .setMessage(R.string.settings_is_clean_env_summary)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            XposedUtils.stopSystemService(activity, true)
                            Toast.makeText(activity, R.string.settings_stop_system_service, Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton(R.string.no) { _, _ ->
                            XposedUtils.stopSystemService(activity, false)
                            Toast.makeText(activity, R.string.settings_stop_system_service, Toast.LENGTH_SHORT).show()
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                } else Toast.makeText(activity, R.string.xposed_service_off, Toast.LENGTH_SHORT).show()
                true
            }
            preferenceScreen.findPreference<Preference>("ForceCleanEnv")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.settings_force_clean_env)
                    .setMessage(R.string.settings_is_clean_env_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val result = Shell.su("rm -rf /data/misc/hide_my_applist /data/misc/hma_selinux_test").exec().isSuccess
                        if (result) Toast.makeText(activity, R.string.settings_force_clean_env_toast_successful, Toast.LENGTH_SHORT).show()
                        else Toast.makeText(activity, R.string.settings_force_clean_env_toast_failed, Toast.LENGTH_SHORT).show()
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