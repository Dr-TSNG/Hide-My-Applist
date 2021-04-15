package com.tsng.hidemyapplist.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.tsng.hidemyapplist.R
import kotlin.collections.set

class ScopeManageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scope_manage)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.xposed_score_container, SettingsFragment())
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var map: MutableMap<String, String>
        private lateinit var templates: Set<String>

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "Scope"
            preferenceManager.sharedPreferencesMode = MODE_WORLD_READABLE
            setPreferencesFromResource(R.xml.scope_preferences, rootKey)
            map = preferenceManager.sharedPreferences.all as MutableMap<String, String>
            templates = setOf("<close>") +
                    requireActivity().getSharedPreferences("Templates", MODE_WORLD_READABLE).getStringSet("List", setOf())!!
            refresh()
        }

        private fun refresh() {
            val packages = requireActivity().packageManager.getInstalledPackages(0)
            packages.removeAll { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            packages.sortWith { o1, o2 ->
                val l1 = o1.applicationInfo.loadLabel(requireActivity().packageManager) as String
                val l2 = o2.applicationInfo.loadLabel(requireActivity().packageManager) as String
                if (map.containsKey(o1.packageName) xor map.containsKey(o2.packageName))
                    if (map.containsKey(o2.packageName)) 1
                    else -1
                else
                    l1.compareTo(l2)
            }
            preferenceScreen.removeAll()
            for (pkg in packages)
                preferenceScreen.addPreference(ListPreference(context).apply {
                    dialogTitle = getString(R.string.xposed_teplate_select)
                    title = pkg.applicationInfo.loadLabel(requireActivity().packageManager)
                    icon = pkg.applicationInfo.loadIcon(requireActivity().packageManager)
                    key = pkg.packageName
                    entries = templates.toTypedArray()
                    entryValues = templates.toTypedArray()
                    if (map.containsKey(pkg.packageName))
                        summary = map[pkg.packageName]
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue.equals("<close>")) {
                            summary = null
                            map.remove(pkg.packageName)
                            value = "<close>"
                            preferenceManager.sharedPreferences.edit().remove(pkg.packageName).apply()
                            false
                        } else {
                            summary = newValue as String
                            map[pkg.packageName] = newValue
                            true
                        }
                    }
                })
        }
    }
}