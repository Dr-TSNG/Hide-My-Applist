package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialSharedAxis
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppManageAdapter
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSelectViewModel

class AppManageFragment : AppSelectFragment() {

    override val viewModel by viewModels<AppSelectViewModel> {
        AppSelectViewModel.Factory(
            firstComparator = Comparator.comparing(ConfigManager::isUsingHide).reversed(),
            adapter = AppManageAdapter()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }
}
