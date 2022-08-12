package icu.nullptr.hidemyapplist.ui.fragment

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
                "disableUpdate" -> PrefManager.disableUpdate
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun getString(key: String, defValue: String?): String {
            return when (key) {
                "maxLogSize" -> ConfigManager.maxLogSize.toString()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putBoolean(key: String, value: Boolean) {
            when (key) {
                "detailLog" -> ConfigManager.detailLog = value
                "hideIcon" -> PrefManager.hideIcon = value
                "disableUpdate" -> PrefManager.disableUpdate = value
                "receiveBetaUpdate" -> PrefManager.receiveBetaUpdate = value
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }

        override fun putString(key: String, value: String?) {
            when (key) {
                "maxLogSize" -> ConfigManager.maxLogSize = value!!.toInt()
                else -> throw IllegalArgumentException("Invalid key: $key")
            }
        }
    }

    class SettingsPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = SettingsPreferenceDataStore()
            setPreferencesFromResource(R.xml.settings, rootKey)
            findPreference<SimpleMenuPreference>("maxLogSize")?.let {
                it.summary = it.entry
                it.setOnPreferenceChangeListener { _, newValue ->
                    it.summary = it.entries[it.findIndexOfValue(newValue as String)]
                    true
                }
            }
        }
    }
}
