package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppScopeAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSelectViewModel

class ScopeFragment : AppSelectFragment() {

    private lateinit var checked: MutableSet<String>

    override val viewModel by viewModels<AppSelectViewModel> {
        val args by navArgs<ScopeFragmentArgs>()
        checked = args.checked.toMutableSet()
        AppSelectViewModel.Factory(
            firstComparator = Comparator.comparing { !checked.contains(it) },
            adapter = run {
                if (!args.filterOnlyEnabled) AppScopeAdapter(checked, null)
                else AppScopeAdapter(checked) { ConfigManager.getAppConfig(it)?.useWhitelist == args.isWhiteList }
            }
        )
    }

    override fun onBack() {
        setFragmentResult("app_select", Bundle().apply {
            putStringArrayList("checked", ArrayList(checked))
        })
        navController.navigateUp()
    }
}
