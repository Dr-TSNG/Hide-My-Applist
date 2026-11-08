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
                    val appliedAppList = ConfigManager.getTemplateAppliedAppList(it)
                    val targetAppList = ConfigManager.getTemplateTargetAppList(it)
                    viewModel.appliedAppList.value = appliedAppList
                    viewModel.targetAppList.value = targetAppList
                    viewModel.originalAppliedAppList.addAll(appliedAppList)
                    viewModel.originalTargetAppList.addAll(targetAppList)
                }
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val appliedAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
    val targetAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
    private val originalAppliedAppList = ArrayList<String>()
    private val originalTargetAppList = ArrayList<String>()

    fun hasUnsavedChanges(): Boolean {
        return name?.trim().orEmpty() != originalName.orEmpty() ||
                appliedAppList.value.toSet() != originalAppliedAppList.toSet() ||
                targetAppList.value.toSet() != originalTargetAppList.toSet()
    }
}
