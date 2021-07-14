package com.tsng.hidemyapplist.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.deepCopy
import com.tsng.hidemyapplist.app.makeToast
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.views.MapsRulesView
import com.tsng.hidemyapplist.databinding.FragmentTemplateSettingsBinding

class TemplateSettingsFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(isWhitelist: Boolean, templateName: String?) =
            TemplateSettingsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isWhitelist", isWhitelist)
                    putString("templateName", templateName)
                }
            }
    }

    private lateinit var binding: FragmentTemplateSettingsBinding
    private lateinit var activity: Activity
    private lateinit var template: JsonConfig.Template
    private var oldTemplateName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity = requireActivity()
        val isWhitelist = requireArguments().getBoolean("isWhitelist")
        oldTemplateName = requireArguments().getString("templateName")
        template = if (oldTemplateName == null) JsonConfig.Template(isWhitelist)
        else globalConfig.templates[oldTemplateName]!!.deepCopy()

        setFragmentResultListener("appSelectResult") { _, bundle ->
            bundle.getStringArray("selectedApps")?.let {
                template.appList.clear()
                template.appList.addAll(it)
                binding.appList.setListCount(template.appList.size)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplateSettingsBinding.inflate(inflater, container, false)
        if (oldTemplateName != null) binding.templateName.setText(oldTemplateName)
        binding.appList.setRawText(
            if (template.isWhitelist) getString(R.string.template_apps_visible_count)
            else getString(R.string.template_apps_invisible_count)
        )
        initAppListView()
        initMapsRulesView()
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_and_save, menu)
        if (oldTemplateName == null)
            menu.findItem(R.id.toolbar_delete).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_delete -> {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.template_delete)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        JsonConfigManager.edit {
                            for ((_, appConfig) in scope)
                                appConfig.applyHooks.remove(oldTemplateName)
                            templates.remove(oldTemplateName)
                        }
                        activity.onBackPressed()
                    }
                    .show()
                return true
            }
            R.id.toolbar_save -> {
                val newTemplateName = binding.templateName.text.toString()
                if (newTemplateName.isEmpty()) {
                    makeToast(R.string.template_name_cannot_be_empty)
                    return true
                }
                if (oldTemplateName == null) { // A new template
                    if (globalConfig.templates.containsKey(newTemplateName)) {
                        makeToast(R.string.template_name_already_exist)
                    } else {
                        JsonConfigManager.edit { templates[newTemplateName] = template }
                        activity.onBackPressed()
                    }
                } else when { // Existing template
                    oldTemplateName == newTemplateName -> {
                        JsonConfigManager.edit { templates[newTemplateName] = template }
                        activity.onBackPressed()
                    }
                    globalConfig.templates.containsKey(newTemplateName) -> {
                        makeToast(R.string.template_name_already_exist)
                    }
                    else -> {
                        JsonConfigManager.edit {
                            for ((_, appConfig) in scope)
                                with(appConfig.applyTemplates) {
                                    if (contains(oldTemplateName)) {
                                        remove(oldTemplateName)
                                        add(newTemplateName)
                                    }
                                }
                            templates.remove(oldTemplateName)
                            templates[newTemplateName] = template
                        }
                        activity.onBackPressed()
                    }
                }
                return true
            }
            else -> return false
        }
    }

    private fun initAppListView() {
        var cnt = 0
        globalConfig.scope.forEach { (_, appConfig) ->
            if (appConfig.applyTemplates.contains(oldTemplateName)) cnt++
        }
        binding.effectiveApps.setListCount(cnt)
        with(binding.appList) {
            setListCount(template.appList.size)
            setOnButtonClickListener {
                startFragment(AppSelectFragment.newInstance(template.appList.toTypedArray()))
            }
        }
    }

    private fun initMapsRulesView() {
        with(binding.mapsRules) {
            setListCount(template.mapsRules.size)
            setOnButtonClickListener {
                MapsRulesView.show(activity, template.mapsRules) {
                    setListCount(template.mapsRules.size)
                }
            }
        }
    }
}