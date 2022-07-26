package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.material.transition.MaterialElevationScale
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentHomeBinding
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ServiceHelper
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding<FragmentHomeBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialElevationScale(false)
        reenterTransition = MaterialElevationScale(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(binding.toolbar, getString(R.string.app_name))
        binding.adBanner.loadAd(AdRequest.Builder().build())
        binding.templateManage.setOnClickListener {
            val extras = FragmentNavigatorExtras(binding.manageCard to "transition_manage")
            navController.navigate(R.id.nav_template_manage, null, null, extras)
        }
        binding.appManage.setOnClickListener {
            val extras = FragmentNavigatorExtras(binding.manageCard as View to "transition_manage")
            navController.navigate(R.id.nav_app_manage, null, null, extras)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hmaApp.isHooked) {
            binding.moduleStatusIcon.setImageResource(R.drawable.outline_done_all_24)
            binding.moduleStatus.text = String.format(getString(R.string.xposed_activated), BuildConfig.VERSION_CODE)
        } else {
            binding.moduleStatusIcon.setImageResource(R.drawable.outline_extension_off_24)
            binding.moduleStatus.setText(R.string.xposed_not_activated)
        }
        val serviceVersion = ServiceHelper.getServiceVersion()
        if (serviceVersion != 0) {
            binding.serviceStatus.text = String.format(getString(R.string.xposed_service_on), serviceVersion)
            binding.filterCount.visibility = View.VISIBLE
            binding.filterCount.text = String.format(getString(R.string.xposed_filter_count), ServiceHelper.getFilterCount())
        } else {
            binding.serviceStatus.setText(R.string.xposed_service_off)
            binding.filterCount.visibility = View.GONE
        }
    }
}
