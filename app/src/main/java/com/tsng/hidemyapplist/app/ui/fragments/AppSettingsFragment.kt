package com.tsng.hidemyapplist.app.ui.fragments

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.helpers.AppConfigDataStorage
import com.tsng.hidemyapplist.app.makeToast
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.views.FilterRulesView
import icu.nullptr.hidemyapplist.common.JsonConfig

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        packageName = requireArguments().getString("packageName")!!
        appConfig = globalConfig.scope.getOrDefault(packageName, JsonConfig.AppConfig())
        preferenceManager.preferenceDataStore = AppConfigDataStorage(packageName, appConfig)
        setPreferencesFromResource(R.xml.app_preferences, rootKey)

        preferenceScreen.findPreference<Preference>("appInfo")?.let {
            val appInfo = appContext.packageManager.getApplicationInfo(
                packageName,
                PackageManager.MATCH_UNINSTALLED_PACKAGES or PackageManager.MATCH_DISABLED_COMPONENTS
            )
            it.icon = appInfo.loadIcon(appContext.packageManager)
            it.title = appInfo.loadLabel(appContext.packageManager)
            it.summary = packageName
        }

        preferenceScreen.findPreference<Preference>("copyConfig")
            ?.setOnPreferenceClickListener {
                startFragment(AppSelectFragment.newInstance(arrayOf(packageName), "copyConfig"))
                true
            }

        setFragmentResultListener("copyConfig") { _, bundle ->
            bundle.getStringArray("selectedApps")?.let { arr ->
                JsonConfigManager.edit {
                    arr.forEach { scope[it] = appConfig }
                }
                makeToast(R.string.copied)
            }
        }

        preferenceScreen.findPreference<SwitchPreferenceCompat>("useWhitelist")
            ?.setOnPreferenceClickListener {
                appConfig.applyTemplates.clear()
                appConfig.extraAppList.clear()
                updateView()
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
                startFragment(
                    AppSelectFragment.newInstance(
                        appConfig.extraAppList.toTypedArray(),
                        "extraAppList"
                    )
                )
                true
            }

        setFragmentResultListener("extraAppList") { _, bundle ->
            bundle.getStringArray("selectedApps")?.let {
                appConfig.extraAppList.clear()
                appConfig.extraAppList.addAll(it)
                updateView()
            }
        }

        preferenceScreen.findPreference<Preference>("extraQueryParamRules")
            ?.setOnPreferenceClickListener {
                FilterRulesView.show(requireActivity(), appConfig.extraQueryParamRules) {
                    it.title = getString(
                        R.string.template_extra_query_param_rules_count,
                        appConfig.extraQueryParamRules.size
                    )
                }
                true
            }

        updateView()
    }

    private fun updateView() {
        preferenceScreen.findPreference<Preference>("applyTemplates")?.title =
            getString(R.string.template_applied_count, appConfig.applyTemplates.size)

        preferenceScreen.findPreference<Preference>("extraAppList")?.title =
            getString(
                if (appConfig.useWhitelist) R.string.template_extra_apps_visible_count
                else R.string.template_extra_apps_invisible_count,
                appConfig.extraAppList.size
            )

        preferenceScreen.findPreference<Preference>("extraQueryParamRules")?.title =
            getString(
                R.string.template_extra_query_param_rules_count,
                appConfig.extraQueryParamRules.size
            )
    }
}
