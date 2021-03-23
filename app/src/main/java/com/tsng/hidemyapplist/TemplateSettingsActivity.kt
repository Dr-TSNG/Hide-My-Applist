package com.tsng.hidemyapplist

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class TemplateSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment().apply {
                        arguments = Bundle().apply { putString("template", intent.getStringExtra("template")) }
                    })
                    .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        Toast.makeText(this, getString(R.string.xposed_restart_to_apply), Toast.LENGTH_SHORT).show();
        super.onBackPressed()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "tpl_" + arguments?.getString("template")
            setPreferencesFromResource(R.xml.template_preferences, rootKey)
        }
    }
}