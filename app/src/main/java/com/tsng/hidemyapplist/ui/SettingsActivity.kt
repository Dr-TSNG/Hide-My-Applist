package com.tsng.hidemyapplist.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.xposed.XposedUtils
import kotlinx.android.synthetic.main.toolbar.*

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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "Settings"
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            preferenceScreen.findPreference<SwitchPreferenceCompat>("HookSelf")?.apply {
                setOnPreferenceClickListener {
                    Toast.makeText(requireContext(), R.string.settings_hook_self_toast, Toast.LENGTH_SHORT).show()
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
                val component = ComponentName(requireContext(), "com.tsng.hidemyapplist.MainActivityLauncher")
                val status = if (newValue == true) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                requireContext().packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
                true
            }
            preferenceScreen.findPreference<Preference>("StopSystemService")?.setOnPreferenceClickListener {
                if (XposedUtils.getServiceVersion(requireContext()) != 0) {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.settings_is_clean_env)
                            .setMessage(R.string.settings_is_clean_env_summary)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                XposedUtils.stopSystemService(requireContext(), true)
                                Toast.makeText(requireContext(), R.string.settings_stop_system_service, Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton(R.string.no) { _, _ ->
                                XposedUtils.stopSystemService(requireContext(), false)
                                Toast.makeText(requireContext(), R.string.settings_stop_system_service, Toast.LENGTH_SHORT).show()
                            }
                            .setNeutralButton(android.R.string.cancel, null)
                            .show()
                } else Toast.makeText(requireContext(), R.string.xposed_service_off, Toast.LENGTH_SHORT).show()
                true
            }
            preferenceScreen.findPreference<Preference>("ForceCleanEnv")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_force_clean_env)
                        .setMessage(R.string.settings_is_clean_env_summary)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val result = Shell.su("rm -rf /data/misc/hide_my_applist /data/misc/hma_selinux_test").exec().isSuccess
                            if (result) Toast.makeText(requireContext(), R.string.settings_force_clean_env_toast_success, Toast.LENGTH_SHORT).show()
                            else Toast.makeText(requireContext(), R.string.settings_force_clean_env_toast_fail, Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                true
            }
        }
    }
}