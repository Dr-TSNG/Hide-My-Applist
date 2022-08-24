package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentSettingsBinding
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import rikka.material.app.DayNightDelegate
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference

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
                "detailLog" -> ConfigManager.detailLog
                "hideIcon" -> PrefManager.hideIcon
                "forceMountData" -> ConfigManager.forceMountData
                "followSystemAccent" -> PrefManager.followSystemAccent
                "blackDarkTheme" -> PrefManager.blackDarkTheme
                "disableUpdate" -> PrefManager.disableUpdate
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getString(key: String, defValue: String?): String {
            return when (key) {
                "maxLogSize" -> ConfigManager.maxLogSize.toString()
                "themeColor" -> PrefManager.themeColor
                "darkTheme" -> PrefManager.darkTheme.toString()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "detailLog" -> ConfigManager.detailLog = value
                "forceMountData" -> ConfigManager.forceMountData = value
                "hideIcon" -> PrefManager.hideIcon = value
                "followSystemAccent" -> PrefManager.followSystemAccent = value
                "blackDarkTheme" -> PrefManager.blackDarkTheme = value
                "disableUpdate" -> PrefManager.disableUpdate = value
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String, value: String?) {
            when (key) {
                "maxLogSize" -> ConfigManager.maxLogSize = value!!.toInt()
                "themeColor" -> PrefManager.themeColor = value!!
                "darkTheme" -> PrefManager.darkTheme = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings, rootKey)

            findPreference<MaterialSwitchPreference>("forceMountData")?.let {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    it.isEnabled = false
                }
            }

            findPreference<MaterialSwitchPreference>("followSystemAccent")
                ?.setOnPreferenceChangeListener { _, _ ->
                    activity?.recreate()
                    true
                }

            findPreference<SimpleMenuPreference>("themeColor")
                ?.setOnPreferenceChangeListener { _, _ ->
                    activity?.recreate()
                    true
                }

            findPreference<SimpleMenuPreference>("darkTheme")
                ?.setOnPreferenceChangeListener { _, newValue ->
                    val newMode = (newValue as String).toInt()
                    if (PrefManager.darkTheme != newMode) {
                        DayNightDelegate.setDefaultNightMode(newMode)
                        activity?.recreate()
                    }
                    true
                }

            findPreference<MaterialSwitchPreference>("blackDarkTheme")
                ?.setOnPreferenceChangeListener { _, _ ->
                    activity?.recreate()
                    true
                }
        }
    }
}
