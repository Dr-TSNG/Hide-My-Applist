package icu.nullptr.hidemyapplist.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class TemplateSettingsViewModel : ViewModel() {

    var name: String? = null
    var originalName: String? = null
    var isWhiteList: Boolean = false

    val appliedAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
    val targetAppList = MutableStateFlow<ArrayList<String>>(ArrayList())
}
