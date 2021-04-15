package com.tsng.hidemyapplist.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.tsng.hidemyapplist.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
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
            preferenceManager.sharedPreferencesMode = MODE_WORLD_READABLE
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
        }
    }
}