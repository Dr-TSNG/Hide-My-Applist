package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialSharedAxis
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.AppManageAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSelectViewModel

class AppManageFragment : AppSelectFragment() {

    override val viewModel by viewModels<AppSelectViewModel> {
        AppSelectViewModel.Factory(
            firstComparator = Comparator.comparing(ConfigManager::isHideEnabled).reversed(),
            adapter = AppManageAdapter {
                val args = AppSettingsFragmentArgs(it)
                navController.navigate(R.id.nav_app_settings, args.toBundle())
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }
}
