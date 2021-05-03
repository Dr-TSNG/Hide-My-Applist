package com.tsng.hidemyapplist.ui

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.iterator
import com.tsng.hidemyapplist.R
import kotlinx.android.synthetic.main.appselect.*
import kotlinx.android.synthetic.main.toolbar.*
import java.text.Collator
import java.util.*
import kotlin.concurrent.thread


class TemplateSettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var template: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appselect)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        template = intent.getStringExtra("template")
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.appselect_container, SettingsFragment().apply {
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
                .replace(R.id.appselect_container, fragment)
                .addToBackStack(null)
                .commit()
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = "tpl_" + arguments?.getString("template")
            setPreferencesFromResource(R.xml.template_preferences, rootKey)
        }
    }

    class HideAppsFragment : PreferenceFragmentCompat() {
        private lateinit var list: MutableSet<String>

        private var showSystemApps = false
        private var refreshThread: Thread? = null

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.toolbar_appselect, menu)
            menu.findItem(R.id.toolbar_select_all_apps).isVisible = true
            val searchView = menu.findItem(R.id.toolbar_search).actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    for (pref in preferenceScreen)
                        pref.isVisible = pref.title.contains(newText)
                    return false
                }
            })
        }

        @SuppressLint("RestrictedApi")
        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.toolbar_show_system_apps -> {
                    item.isChecked = !item.isChecked
                    showSystemApps = item.isChecked
                    requireActivity().refresh_layout.autoRefresh()
                    true
                }
                R.id.toolbar_select_all_apps -> {
                    var anti = true
                    for (pref in preferenceScreen)
                        if (!(pref as CheckBoxPreference).isChecked) anti = false
                    for (pref in preferenceScreen)
                        if (!(anti xor (pref as CheckBoxPreference).isChecked))
                            pref.performClick()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setHasOptionsMenu(true)
            preferenceManager.sharedPreferencesName = "tpl_" + arguments?.getString("template")
            setPreferencesFromResource(R.xml.empty_preferences, rootKey)
            list = preferenceManager.sharedPreferences.getStringSet("HideApps", setOf())!!.toMutableSet()
            requireActivity().refresh_layout.apply {
                setOnRefreshListener { refresh() }
                autoRefresh()
            }
        }

        override fun onDestroy() {
            refreshThread?.let {
                it.interrupt()
                while (it.isAlive)
                    Thread.sleep(50)
            }
            super.onDestroy()
        }

        private fun refresh() {
            refreshThread = thread {
                try {
                    val packages = requireActivity().packageManager.getInstalledPackages(0)
                    if (!showSystemApps)
                        packages.removeAll { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 && !list.contains(it.packageName) }
                    packages.sortWith { o1, o2 ->
                        if (Thread.interrupted()) throw InterruptedException()
                        val l1 = o1.applicationInfo.loadLabel(requireActivity().packageManager) as String
                        val l2 = o2.applicationInfo.loadLabel(requireActivity().packageManager) as String
                        if (list.contains(o1.packageName) xor list.contains(o2.packageName))
                            if (list.contains(o2.packageName)) 1
                            else -1
                        else
                            Collator.getInstance(Locale.getDefault()).compare(l1, l2)
                    }
                    preferenceScreen.removeAll()
                    for (pkg in packages) {
                        if (Thread.interrupted()) throw InterruptedException()
                        preferenceScreen.addPreference(CheckBoxPreference(context).apply {
                            title = if (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                                getString(R.string.template_is_system_app) + " - " + pkg.applicationInfo.loadLabel(requireActivity().packageManager)
                            else
                                pkg.applicationInfo.loadLabel(requireActivity().packageManager)
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
                    requireActivity().refresh_layout.finishRefresh()
                } catch (e: InterruptedException) { }
            }
        }
    }
}