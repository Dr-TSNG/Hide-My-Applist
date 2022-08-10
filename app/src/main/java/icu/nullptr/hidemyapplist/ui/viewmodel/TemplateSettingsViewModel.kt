package icu.nullptr.hidemyapplist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.fragment.TemplateSettingsFragmentArgs
import kotlinx.coroutines.flow.MutableStateFlow

class TemplateSettingsViewModel(
    val originalName: String?,
    val isWhiteList: Boolean,
    var name: String?
) : ViewModel() {

    class Factory(private val args: TemplateSettingsFragmentArgs) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TemplateSettingsViewModel::class.java)) {
                val viewModel = TemplateSettingsViewModel(args.name, args.isWhiteList, args.name)
                args.name?.let {
                    viewModel.appliedAppList.value = ConfigManager.getTemplateAppliedAppList(it)
                    viewModel.targetAppList.value = ConfigManager.getTemplateTargetAppList(it)
                }
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val appliedAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
    val targetAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
}
