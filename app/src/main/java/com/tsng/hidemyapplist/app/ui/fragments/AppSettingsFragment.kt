package com.tsng.hidemyapplist.app.ui.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.deepCopy
import com.tsng.hidemyapplist.app.helpers.AppConfigDataStorage
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.views.MapsRulesView

class AppSettingsFragment : PreferenceFragmentCompat() {
    companion object {
        @JvmStatic
        fun newInstance(packageName: String) =
            AppSettingsFragment().apply {
                arguments = bundleOf("packageName" to packageName)
            }
    }

    private lateinit var packageName: String
    private lateinit var appConfig: JsonConfig.AppConfig

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_and_save, menu)
        menu.findItem(R.id.toolbar_delete).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_save -> {
                JsonConfigManager.edit {
                    if ((preferenceManager.preferenceDataStore as AppConfigDataStorage).isEnabled)
                        scope[packageName] = appConfig
                    else scope.remove(packageName)
                    activity?.onBackPressed()
                }
            }
            else -> return false
        }
        return true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setHasOptionsMenu(true)
        packageName = requireArguments().getString("packageName")!!
        appConfig = if (!globalConfig.scope.containsKey(packageName)) JsonConfig.AppConfig()
        else globalConfig.scope[packageName]!!.deepCopy()

        preferenceManager.preferenceDataStore =
            AppConfigDataStorage(appConfig).apply {
                isEnabled = globalConfig.scope.containsKey(packageName)
            }
        setPreferencesFromResource(R.xml.app_preferences, rootKey)

        preferenceScreen.findPreference<Preference>("appInfo")?.let {
            val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
            it.icon = appInfo.loadIcon(appContext.packageManager)
            it.title = appInfo.loadLabel(appContext.packageManager)
            it.summary = packageName
        }

        preferenceScreen.findPreference<SwitchPreferenceCompat>("useWhitelist")
            ?.setOnPreferenceClickListener {
                appConfig.applyTemplates.clear()
                appConfig.extraAppList.clear()
                updateView();
                true
            }

        preferenceScreen.findPreference<Preference>("applyTemplates")
            ?.setOnPreferenceClickListener {
                val list = mutableListOf<String>()
                val checked = mutableListOf<Boolean>()
                for ((name, template) in globalConfig.templates)
                    if (appConfig.useWhitelist == template.isWhitelist) {
                        list.add(name)
                        checked.add(appConfig.applyTemplates.contains(name))
                    }
                val arrayList = list.toTypedArray()
                val arrayChecked = checked.toBooleanArray()
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.template_choose)
                    .setMultiChoiceItems(
                        arrayList,
                        arrayChecked
                    ) { _: DialogInterface, i: Int, b: Boolean -> arrayChecked[i] = b }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        for (i in arrayList.indices)
                            if (arrayChecked[i]) appConfig.applyTemplates.add(arrayList[i])
                            else appConfig.applyTemplates.remove(arrayList[i])
                        updateView()
                    }
                    .show()
                true
            }

        preferenceScreen.findPreference<Preference>("extraAppList")
            ?.setOnPreferenceClickListener {
                startFragment(AppSelectFragment.newInstance(appConfig.extraAppList.toTypedArray()))
                true
            }

        preferenceScreen.findPreference<Preference>("extraMapsRules")
            ?.setOnPreferenceClickListener {
                MapsRulesView.show(requireActivity(), appConfig.extraMapsRules) {
                    it.title = getString(R.string.template_extra_maps_rules_count)
                        .replace(Regex("#"), appConfig.extraMapsRules.size.toString())
                }
                true
            }

        setFragmentResultListener("appSelectResult") { _, bundle ->
            bundle.getStringArray("selectedApps")?.let {
                appConfig.extraAppList.clear()
                appConfig.extraAppList.addAll(it)
                updateView()
            }
        }

        updateView()
    }

    private fun updateView() {
        preferenceScreen.findPreference<Preference>("applyTemplates")?.title =
            getString(R.string.template_applied_count).replace(
                Regex("#"),
                appConfig.applyTemplates.size.toString()
            )

        preferenceScreen.findPreference<Preference>("extraAppList")?.title =
            getString(
                if (appConfig.useWhitelist) R.string.template_extra_apps_visible_count
                else R.string.template_extra_apps_invisible_count
            ).replace(Regex("#"), appConfig.extraAppList.size.toString())

        preferenceScreen.findPreference<Preference>("extraMapsRules")?.title =
            getString(R.string.template_extra_maps_rules_count).replace(
                Regex("#"),
                appConfig.extraMapsRules.size.toString()
            )
    }
}