package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSelectViewModel
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class AppSelectFragment : Fragment(R.layout.fragment_app_select) {

    private val binding by viewBinding<FragmentAppSelectBinding>()
    protected abstract val viewModel: AppSelectViewModel

    protected open fun onBack() {
        navController.navigateUp()
    }

    private fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_show_system -> {
                item.isChecked = !item.isChecked
                PrefManager.filter_showSystem = item.isChecked
            }
            R.id.menu_sort_by_label -> {
                item.isChecked = true
                PrefManager.filter_sortMethod = PrefManager.SortMethod.BY_LABEL
            }
            R.id.menu_sort_by_package_name -> {
                item.isChecked = true
                PrefManager.filter_sortMethod = PrefManager.SortMethod.BY_PACKAGE_NAME
            }
            R.id.menu_sort_by_install_time -> {
                item.isChecked = true
                PrefManager.filter_sortMethod = PrefManager.SortMethod.BY_INSTALL_TIME
            }
            R.id.menu_sort_by_update_time -> {
                item.isChecked = true
                PrefManager.filter_sortMethod = PrefManager.SortMethod.BY_UPDATE_TIME
            }
            R.id.menu_reverse_order -> {
                item.isChecked = !item.isChecked
                PrefManager.filter_reverseOrder = item.isChecked
            }
        }
        viewModel.sortList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_app_select),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() },
            menuRes = R.menu.menu_app_list,
            onMenuOptionSelected = this::onMenuOptionSelected
        )

        with(binding.toolbar.menu) {
            findItem(R.id.menu_show_system).isChecked = PrefManager.filter_showSystem
            when (PrefManager.filter_sortMethod) {
                PrefManager.SortMethod.BY_LABEL -> findItem(R.id.menu_sort_by_label).isChecked = true
                PrefManager.SortMethod.BY_PACKAGE_NAME -> findItem(R.id.menu_sort_by_package_name).isChecked = true
                PrefManager.SortMethod.BY_INSTALL_TIME -> findItem(R.id.menu_sort_by_install_time).isChecked = true
                PrefManager.SortMethod.BY_UPDATE_TIME -> findItem(R.id.menu_sort_by_update_time).isChecked = true
            }
            findItem(R.id.menu_reverse_order).isChecked = PrefManager.filter_reverseOrder
        }
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = viewModel.adapter
        binding.swipeRefresh.setOnRefreshListener {
            PackageHelper.invalidateCache()
        }

        lifecycleScope.launch {
            PackageHelper.isRefreshing
                .flowWithLifecycle(lifecycle)
                .collect {
                    binding.swipeRefresh.isRefreshing = it
                }
        }
    }
}
