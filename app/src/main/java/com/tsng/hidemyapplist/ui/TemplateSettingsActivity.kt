package com.tsng.hidemyapplist.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.tsng.hidemyapplist.R


class TemplateSettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private var template: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_settings)
        template = intent.getStringExtra("template")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.xposed_template_container, SettingsFragment().apply {
                        arguments = Bundle().apply { putString("template", template) }
                    })
                    .commit()
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment)
        fragment.arguments = Bundle().apply { putString("template", template) }
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.xposed_template_container, fragment)
                .addToBackStack(null)
                .commit()
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0)
            Toast.makeText(this, R.string.xposed_restart_to_apply, Toast.LENGTH_SHORT).show();
        super.onBackPressed()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "tpl_" + arguments?.getString("template")
            preferenceManager.sharedPreferencesMode = MODE_WORLD_READABLE
            setPreferencesFromResource(R.xml.template_preferences, rootKey)
        }
    }

    class HideAppsFragment : PreferenceFragmentCompat() {

        private lateinit var list: MutableSet<String>

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "tpl_" + arguments?.getString("template")
            preferenceManager.sharedPreferencesMode = MODE_WORLD_READABLE
            setPreferencesFromResource(R.xml.template_hideapps_preference, rootKey)
            list = preferenceManager.sharedPreferences.getStringSet("HideApps", setOf())!!.toMutableSet()
            refresh()
        }

        private fun refresh() {
            val packages = requireActivity().packageManager.getInstalledPackages(0)
            packages.removeAll { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            packages.sortWith { o1, o2 ->
                val l1 = o1.applicationInfo.loadLabel(requireActivity().packageManager) as String
                val l2 = o2.applicationInfo.loadLabel(requireActivity().packageManager) as String
                if (list.contains(o1.packageName) xor list.contains(o2.packageName))
                    if (list.contains(o2.packageName)) 1
                    else -1
                else
                    l1.compareTo(l2)
            }
            preferenceScreen.removeAll()
            for (pkg in packages)
                preferenceScreen.addPreference(CheckBoxPreference(context).apply {
                    title = pkg.applicationInfo.loadLabel(requireActivity().packageManager)
                    icon = pkg.applicationInfo.loadIcon(requireActivity().packageManager)
                    isChecked = list.contains(pkg.packageName)
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue == true)
                            list.add(pkg.packageName)
                        else
                            list.remove(pkg.packageName)
                        preferenceManager.sharedPreferences.edit().putStringSet("HideApps", list.toSet()).apply()
                        true
                    }
                })
        }
    }
}