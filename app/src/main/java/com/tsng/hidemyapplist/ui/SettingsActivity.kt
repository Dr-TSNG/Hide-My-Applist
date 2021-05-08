package com.tsng.hidemyapplist.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.tsng.hidemyapplist.R
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
            preferenceScreen.findPreference<ListPreference>("HookMode")?.apply {
                summary = entry
                setOnPreferenceChangeListener { _, newValue ->
                    summary = newValue as CharSequence?
                    if (value != newValue)
                        Toast.makeText(activity, R.string.settings_need_reboot, Toast.LENGTH_SHORT).show()
                    true
                }
            }
            preferenceScreen.findPreference<SwitchPreferenceCompat>("HideIcon")?.setOnPreferenceChangeListener { _, newValue ->
                val component = ComponentName(requireContext(), "com.tsng.hidemyapplist.MainActivityLauncher")
                val status = if (newValue == true) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                requireActivity().packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
                true
            }
        }
    }
}