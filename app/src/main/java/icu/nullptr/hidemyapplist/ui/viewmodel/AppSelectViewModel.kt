package icu.nullptr.hidemyapplist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icu.nullptr.hidemyapplist.ui.adapter.AppSelectAdapter
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.launch

class AppSelectViewModel(
    private val firstComparator: Comparator<String>,
    val adapter: AppSelectAdapter
) : ViewModel() {

    class Factory(
        private val firstComparator: Comparator<String>,
        private val adapter: AppSelectAdapter
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppSelectViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppSelectViewModel(firstComparator, adapter) as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    var search = ""

    init {
        sortList()
    }

    fun applyFilter() {
        adapter.filter.filter(search)
    }

    fun sortList() {
        viewModelScope.launch {
            PackageHelper.sortList(firstComparator)
            applyFilter()
        }
    }
}
