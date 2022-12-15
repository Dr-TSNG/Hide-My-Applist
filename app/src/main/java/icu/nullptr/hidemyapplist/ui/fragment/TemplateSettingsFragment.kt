package icu.nullptr.hidemyapplist.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentTemplateSettingsBinding
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.viewmodel.TemplateSettingsViewModel
import kotlinx.coroutines.launch

class TemplateSettingsFragment : Fragment(R.layout.fragment_template_settings) {

    private val binding by viewBinding<FragmentTemplateSettingsBinding>()
    private val viewModel by viewModels<TemplateSettingsViewModel> {
        val args by navArgs<TemplateSettingsFragmentArgs>()
        TemplateSettingsViewModel.Factory(args)
    }

    private fun onBack(delete: Boolean) {
        viewModel.name = viewModel.name?.trim()
        if (viewModel.name != viewModel.originalName && (ConfigManager.hasTemplate(viewModel.name) || viewModel.name == null) || delete) {
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle(if (delete) R.string.template_delete_title else R.string.template_name_invalid)
                .setMessage(if (delete) R.string.template_delete else R.string.template_name_already_exist)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    saveResult(delete)
                }
            if (delete) builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
        } else {
            saveResult(false)
        }
    }

    private fun saveResult(delete: Boolean) {
        setFragmentResult("template_settings", Bundle().apply {
            putString("name",if (delete) null else viewModel.name)
            putStringArrayList("appliedList", viewModel.appliedAppList.value)
            putStringArrayList("targetList", viewModel.targetAppList.value)
        })
        navController.navigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack(false) }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_template_settings),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack(false) },
            menuRes = R.menu.menu_delete,
            onMenuOptionSelected = {
                onBack(true)
            }
        )

        binding.templateName.setText(viewModel.name)
        binding.templateName.addTextChangedListener { viewModel.name = it.toString() }
        binding.targetApps.setOnClickListener {
            setFragmentResultListener("app_select") { _, bundle ->
                viewModel.targetAppList.value = bundle.getStringArrayList("checked")!!
                clearFragmentResultListener("app_select")
            }
            val args = ScopeFragmentArgs(
                filterOnlyEnabled = false,
                checked = viewModel.targetAppList.value.toTypedArray()
            )
            navController.navigate(R.id.nav_scope, args.toBundle())
        }
        binding.appliedApps.setOnClickListener {
            setFragmentResultListener("app_select") { _, bundle ->
                viewModel.appliedAppList.value = bundle.getStringArrayList("checked")!!
                clearFragmentResultListener("app_select")
            }
            val args = ScopeFragmentArgs(
                filterOnlyEnabled = true,
                isWhiteList = viewModel.isWhiteList,
                checked = viewModel.appliedAppList.value.toTypedArray()
            )
            navController.navigate(R.id.nav_scope, args.toBundle())
        }

        lifecycleScope.launch {
            viewModel.targetAppList.collect {
                val fmt =
                    if (viewModel.isWhiteList) R.string.template_apps_visible_count
                    else R.string.template_apps_invisible_count
                binding.targetApps.text = String.format(getString(fmt), it.size)
            }
        }
        lifecycleScope.launch {
            viewModel.appliedAppList.collect {
                binding.appliedApps.text = String.format(getString(R.string.template_applied_count), it.size)
            }
        }
    }
}
