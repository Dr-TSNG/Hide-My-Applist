package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.*
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentSettingsBinding
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSettingsViewModel
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppSettingsFragment : Fragment(R.layout.fragment_settings) {

    private val binding by viewBinding<FragmentSettingsBinding>()
    private val viewModel by viewModels<AppSettingsViewModel>() {
        val args by navArgs<AppSettingsFragmentArgs>()
        val cfg = ConfigManager.getAppConfig(args.packageName)
        val pack = if (cfg != null) AppSettingsViewModel.Pack(args.packageName, true, cfg)
        else AppSettingsViewModel.Pack(args.packageName, false, JsonConfig.AppConfig())
        AppSettingsViewModel.Factory(pack)
    }

    private fun onBack() {
        if (!viewModel.pack.enabled) ConfigManager.setAppConfig(viewModel.pack.app, null)
        else ConfigManager.setAppConfig(viewModel.pack.app, viewModel.pack.config)
        navController.navigateUp()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_app_settings),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() }
        )

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, AppPreferenceFragment())
                .commit()
        }
    }

    class AppPreferenceDataStore(private val pack: AppSettingsViewModel.Pack) : PreferenceDataStore() {

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "enableHide" -> pack.enabled
                "useWhiteList" -> pack.config.useWhitelist
                "excludeSystemApps" -> pack.config.excludeSystemApps
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "enableHide" -> pack.enabled = value
                "useWhiteList" -> pack.config.useWhitelist = value
                "excludeSystemApps" -> pack.config.excludeSystemApps = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class AppPreferenceFragment : PreferenceFragmentCompat() {

        private val parent get() = requireParentFragment() as AppSettingsFragment
        private val pack get() = parent.viewModel.pack

        private fun updateApplyTemplates() {
            findPreference<Preference>("applyTemplates")?.title =
                getString(R.string.app_template_using, pack.config.applyTemplates.size)
        }

        private fun updateExtraAppList(useWhiteList: Boolean) {
            findPreference<Preference>("extraAppList")?.title =
                if (useWhiteList) getString(R.string.app_extra_apps_visible_count, pack.config.extraAppList.size)
                else getString(R.string.app_extra_apps_invisible_count, pack.config.extraAppList.size)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = AppPreferenceDataStore(pack)
            setPreferencesFromResource(R.xml.app_settings, rootKey)
            findPreference<Preference>("appInfo")?.let {
                it.icon = PackageHelper.loadAppIcon(pack.app).toDrawable(resources)
                it.title = PackageHelper.loadAppLabel(pack.app)
                it.summary = PackageHelper.loadPackageInfo(pack.app).packageName
            }
            findPreference<SwitchPreference>("useWhiteList")?.setOnPreferenceChangeListener { _, newValue ->
                pack.config.applyTemplates.clear()
                pack.config.extraAppList.clear()
                updateApplyTemplates()
                updateExtraAppList(newValue as Boolean)
                true
            }
            findPreference<Preference>("applyTemplates")?.setOnPreferenceClickListener {
                val templates = ConfigManager.getTemplateList().mapNotNull {
                    if (it.isWhiteList == pack.config.useWhitelist) it.name else null
                }.toTypedArray()
                val checked = templates.map {
                    pack.config.applyTemplates.contains(it)
                }.toBooleanArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.app_choose_template)
                    .setMultiChoiceItems(templates, checked) { _, i, value -> checked[i] = value }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        pack.config.applyTemplates = templates.mapIndexedNotNullTo(mutableSetOf()) { i, name ->
                            if (checked[i]) name else null
                        }
                        updateApplyTemplates()
                    }
                    .show()
                true
            }
            findPreference<Preference>("extraAppList")?.setOnPreferenceClickListener {
                parent.setFragmentResultListener("app_select") { _, bundle ->
                    pack.config.extraAppList = bundle.getStringArrayList("checked")!!.toMutableSet()
                    updateExtraAppList(pack.config.useWhitelist)
                    parent.clearFragmentResultListener("app_select")
                }

                val args = ScopeFragmentArgs(
                    filterOnlyEnabled = false,
                    checked = pack.config.extraAppList.toTypedArray()
                )
                parent.navController.navigate(R.id.nav_scope, args.toBundle())
                true
            }
            updateApplyTemplates()
            updateExtraAppList(pack.config.useWhitelist)
        }
    }
}
