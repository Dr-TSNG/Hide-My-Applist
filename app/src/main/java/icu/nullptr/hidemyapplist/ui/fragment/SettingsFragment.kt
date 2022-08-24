package icu.nullptr.hidemyapplist.ui.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentSettingsBinding
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceHelper
import icu.nullptr.hidemyapplist.ui.util.makeToast
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.util.LangList
import rikka.material.app.DayNightDelegate
import rikka.material.app.LocaleDelegate
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import java.util.*

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val binding by viewBinding<FragmentSettingsBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(binding.toolbar, getString(R.string.title_settings))

        if (childFragmentManager.findFragmentById(R.id.settings_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsPreferenceFragment())
                .commit()
        }
    }

    class SettingsPreferenceDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return when (key) {
                "followSystemAccent" -> PrefManager.followSystemAccent
                "blackDarkTheme" -> PrefManager.blackDarkTheme
                "detailLog" -> ConfigManager.detailLog
                "hideIcon" -> PrefManager.hideIcon
                "forceMountData" -> ConfigManager.forceMountData
                "disableUpdate" -> PrefManager.disableUpdate
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getString(key: String, defValue: String?): String {
            return when (key) {
                "language" -> PrefManager.locale
                "themeColor" -> PrefManager.themeColor
                "darkTheme" -> PrefManager.darkTheme.toString()
                "maxLogSize" -> ConfigManager.maxLogSize.toString()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "followSystemAccent" -> PrefManager.followSystemAccent = value
                "blackDarkTheme" -> PrefManager.blackDarkTheme = value
                "detailLog" -> ConfigManager.detailLog = value
                "forceMountData" -> ConfigManager.forceMountData = value
                "hideIcon" -> PrefManager.hideIcon = value
                "disableUpdate" -> PrefManager.disableUpdate = value
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String, value: String?) {
            when (key) {
                "language" -> PrefManager.locale = value!!
                "themeColor" -> PrefManager.themeColor = value!!
                "darkTheme" -> PrefManager.darkTheme = value!!.toInt()
                "maxLogSize" -> ConfigManager.maxLogSize = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings, rootKey)

            @Suppress("DEPRECATION")
            findPreference<SimpleMenuPreference>("language")?.let {
                val userLocale = hmaApp.getLocale(PrefManager.locale)
                val entries = buildList {
                    for (lang in LangList.LOCALES) {
                        if (lang == "SYSTEM") add(getString(rikka.core.R.string.follow_system))
                        else {
                            val locale = Locale.forLanguageTag(lang)
                            add(HtmlCompat.fromHtml(locale.getDisplayName(locale), HtmlCompat.FROM_HTML_MODE_LEGACY))
                        }
                    }
                }
                it.entries = entries.toTypedArray()
                it.entryValues = LangList.LOCALES
                if (it.value == "SYSTEM") {
                    it.summary = getString(rikka.core.R.string.follow_system)
                } else {
                    val locale = Locale.forLanguageTag(it.value)
                    it.summary = if (!TextUtils.isEmpty(locale.script)) locale.getDisplayScript(userLocale) else locale.getDisplayName(userLocale)
                }
                it.setOnPreferenceChangeListener { _, newValue ->
                    val locale = hmaApp.getLocale(newValue as String)
                    val config = resources.configuration
                    config.setLocale(locale)
                    LocaleDelegate.defaultLocale = locale
                    hmaApp.resources.updateConfiguration(config, resources.displayMetrics)
                    activity?.recreate()
                    true
                }
            }

            findPreference<Preference>("translation")?.let {
                it.summary = getString(R.string.settings_translate_summary, getString(R.string.app_name))
                it.setOnPreferenceClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Constants.TRANSLATE_URL.toUri()))
                    true
                }
            }

            findPreference<MaterialSwitchPreference>("followSystemAccent")?.setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }

            findPreference<SimpleMenuPreference>("themeColor")?.setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }

            findPreference<SimpleMenuPreference>("darkTheme")?.setOnPreferenceChangeListener { _, newValue ->
                val newMode = (newValue as String).toInt()
                if (PrefManager.darkTheme != newMode) {
                    DayNightDelegate.setDefaultNightMode(newMode)
                    activity?.recreate()
                }
                true
            }

            findPreference<MaterialSwitchPreference>("blackDarkTheme")?.setOnPreferenceChangeListener { _, _ ->
                activity?.recreate()
                true
            }

            findPreference<MaterialSwitchPreference>("forceMountData")
                ?.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

            findPreference<Preference>("stopSystemService")?.setOnPreferenceClickListener {
                if (ServiceHelper.getServiceVersion() != 0) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_is_clean_env)
                        .setMessage(R.string.settings_is_clean_env_summary)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            ServiceHelper.stopSystemService(true)
                            makeToast(R.string.settings_stop_system_service)
                        }
                        .setNegativeButton(R.string.no) { _, _ ->
                            ServiceHelper.stopSystemService(false)
                            makeToast(R.string.settings_stop_system_service)
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .show()
                } else makeToast(R.string.home_xposed_service_off)
                true
            }

            findPreference<Preference>("forceCleanEnv")?.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.settings_force_clean_env)
                    .setMessage(R.string.settings_is_clean_env_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val result = Shell.cmd("rm -rf /data/system/hide_my_applist*").exec().isSuccess && Shell.isAppGrantedRoot() == true
                        if (result) makeToast(R.string.settings_force_clean_env_toast_successful)
                        else makeToast(R.string.settings_force_clean_env_toast_failed)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
        }
    }
}
