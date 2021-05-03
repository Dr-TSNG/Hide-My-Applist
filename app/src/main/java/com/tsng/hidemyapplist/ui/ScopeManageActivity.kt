package com.tsng.hidemyapplist.ui

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.iterator
import com.tsng.hidemyapplist.R
import kotlinx.android.synthetic.main.appselect.*
import kotlinx.android.synthetic.main.toolbar.*
import java.text.Collator
import java.util.*
import kotlin.collections.set
import kotlin.concurrent.thread

class ScopeManageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.appselect)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.appselect_container, SelectAppsFragment())
                    .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SelectAppsFragment : PreferenceFragmentCompat() {
        private lateinit var map: MutableMap<String, String>
        private lateinit var templates: Set<String>

        private var showSystemApps = false
        private var refreshThread: Thread? = null

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            inflater.inflate(R.menu.toolbar_appselect, menu)
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

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.toolbar_show_system_apps -> {
                    item.isChecked = !item.isChecked
                    showSystemApps = item.isChecked
                    requireActivity().refresh_layout.autoRefresh()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setHasOptionsMenu(true)
            preferenceManager.sharedPreferencesName = "Scope"
            setPreferencesFromResource(R.xml.empty_preferences, rootKey)
            map = preferenceManager.sharedPreferences.all as MutableMap<String, String>
            templates = setOf("<close>") +
                    requireActivity().getSharedPreferences("Templates", MODE_PRIVATE).getStringSet("List", setOf())!!
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
                        packages.removeAll { it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 && !map.contains(it.packageName) }
                    packages.sortWith { o1, o2 ->
                        if (Thread.interrupted()) throw InterruptedException()
                        val l1 = o1.applicationInfo.loadLabel(requireActivity().packageManager) as String
                        val l2 = o2.applicationInfo.loadLabel(requireActivity().packageManager) as String
                        if (map.containsKey(o1.packageName) xor map.containsKey(o2.packageName))
                            if (map.containsKey(o2.packageName)) 1
                            else -1
                        else
                            Collator.getInstance(Locale.getDefault()).compare(l1, l2)
                    }
                    preferenceScreen.removeAll()
                    for (pkg in packages) {
                        if (Thread.interrupted()) throw InterruptedException()
                        preferenceScreen.addPreference(ListPreference(context).apply {
                            dialogTitle = getString(R.string.template_select)
                            title = if (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                                getString(R.string.template_is_system_app) + " - " + pkg.applicationInfo.loadLabel(requireActivity().packageManager)
                            else
                                pkg.applicationInfo.loadLabel(requireActivity().packageManager)
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
                    requireActivity().refresh_layout.finishRefresh()
                } catch (e: InterruptedException) { }
            }
        }
    }
}