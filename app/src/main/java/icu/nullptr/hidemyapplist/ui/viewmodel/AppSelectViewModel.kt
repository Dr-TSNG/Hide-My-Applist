package icu.nullptr.hidemyapplist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.adapter.AppAdapter
import icu.nullptr.hidemyapplist.ui.fragment.AppSelectFragmentArgs
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*

class AppSelectViewModel(
    val isMultiSelect: Boolean,
    private val filterOnlyEnabled: Boolean,
    val checked: MutableSet<String>
) : ViewModel() {

    class Factory(private val args: AppSelectFragmentArgs) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppSelectViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppSelectViewModel(args.isMultiSelect, args.filterOnlyEnabled, args.checked.toMutableSet()) as T
            } else throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val adapter = AppAdapter(this)

    var list = listOf<String>()
        private set

    private val comparators = Comparators()

    init {
        viewModelScope.launch {
            PackageHelper.packageCache.collect { cache ->
                val newList = cache.mapTo(mutableListOf()) { it.key }
                sortList(newList)
                list = newList
                adapter.notifyDataSetChanged()
            }
        }
    }

    inline var String.isChecked
        get() = checked.contains(this)
        set(value) {
            if (value) checked.add(this) else checked.remove(this)
        }

    fun sortList() {
        viewModelScope.launch {
            val newList = list.toMutableList()
            sortList(newList)
            list = newList
            adapter.notifyDataSetChanged()
        }
    }

    private fun sortList(listToSort: MutableList<String>) {
        if (filterOnlyEnabled) listToSort.removeIf { !it.isChecked }
        var comparator = when (PrefManager.filter_sortMethod) {
            PrefManager.SortMethod.BY_LABEL -> comparators.byLabel
            PrefManager.SortMethod.BY_PACKAGE_NAME -> comparators.byPackageName
            PrefManager.SortMethod.BY_INSTALL_TIME -> comparators.byInstallTime
            PrefManager.SortMethod.BY_UPDATE_TIME -> comparators.byUpdateTime
        }
        if (PrefManager.filter_reverseOrder) comparator = comparator.reversed()
        listToSort.sortWith(comparators.first.then(comparator))
    }

    private inner class Comparators {
        val first = Comparator<String> { o1, o2 ->
            o2.isChecked.compareTo(o1.isChecked)
        }
        val byLabel = Comparator<String> { o1, o2 ->
            val n1 = PackageHelper.loadAppLabel(o1).lowercase(Locale.getDefault())
            val n2 = PackageHelper.loadAppLabel(o2).lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byPackageName = Comparator<String> { o1, o2 ->
            val n1 = o1.lowercase(Locale.getDefault())
            val n2 = o2.lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byInstallTime = Comparator<String> { o1, o2 ->
            val n1 = PackageHelper.loadPackageInfo(o1).firstInstallTime
            val n2 = PackageHelper.loadPackageInfo(o2).firstInstallTime
            n2.compareTo(n1)
        }
        val byUpdateTime = Comparator<String> { o1, o2 ->
            val n1 = PackageHelper.loadPackageInfo(o1).lastUpdateTime
            val n2 = PackageHelper.loadPackageInfo(o2).lastUpdateTime
            n2.compareTo(n1)
        }
    }
}
