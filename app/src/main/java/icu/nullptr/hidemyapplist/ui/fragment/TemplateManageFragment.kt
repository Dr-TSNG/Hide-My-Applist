package icu.nullptr.hidemyapplist.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.transition.MaterialContainerTransform
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentTemplateManageBinding
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.adapter.TemplateAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar

class TemplateManageFragment : Fragment(R.layout.fragment_template_manage) {

    private val binding by viewBinding<FragmentTemplateManageBinding>()
    private val adapter = TemplateAdapter(this::navigateToSettings)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_template_manage),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { navController.navigateUp() }
        )
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        binding.newBlacklistTemplate.setOnClickListener {
            navigateToSettings(ConfigManager.TemplateInfo(null, false))
        }
        binding.newWhitelistTemplate.setOnClickListener {
            navigateToSettings(ConfigManager.TemplateInfo(null, true))
        }
        binding.templateList.layoutManager = LinearLayoutManager(context)
        binding.templateList.adapter = adapter
    }

    private fun navigateToSettings(info: ConfigManager.TemplateInfo) {
        setFragmentResultListener("template_settings") { _, bundle ->
            fun deal() {
                var name = bundle.getString("name")
                val appliedList = bundle.getStringArrayList("appliedList")!!
                val targetList = bundle.getStringArrayList("targetList")!!
                if (info.name == null) { // New template
                    if (name.isNullOrEmpty()) return
                    ConfigManager.updateTemplate(name, JsonConfig.Template(info.isWhiteList, targetList.toSet()))
                    ConfigManager.updateTemplateAppliedApps(name, appliedList)
                } else {                 // Existing template
                    if (name == null) {
                        ConfigManager.deleteTemplate(info.name)
                    } else {
                        if (name.isEmpty()) name = info.name
                        if (name != info.name) ConfigManager.renameTemplate(info.name, name)
                        ConfigManager.updateTemplate(name, JsonConfig.Template(info.isWhiteList, targetList.toSet()))
                        ConfigManager.updateTemplateAppliedApps(name, appliedList)
                    }
                }
            }
            deal()
            adapter.updateList()
            clearFragmentResultListener("template_settings")
        }

        val args = TemplateSettingsFragmentArgs(info.name, info.isWhiteList)
        val extras = FragmentNavigatorExtras(binding.hintCard to "transition_manage")
        navController.navigate(R.id.nav_template_settings, args.toBundle(), null, extras)
    }
}
