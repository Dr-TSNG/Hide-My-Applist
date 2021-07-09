package com.tsng.hidemyapplist.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.*
import android.widget.*
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
        activity = requireActivity()
        setHasOptionsMenu(true)
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
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun initAppListView() {
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
                val rules = template.mapsRules.toMutableList()
                val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, rules)
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.template_add_maps_rules)
                    .setView(
                        View.inflate(activity, R.layout.alert_customize_maps_rules, null).apply {
                            findViewById<ListView>(R.id.rule_list).apply {
                                this.adapter = adapter
                                setOnItemLongClickListener { _, _, position, _ ->
                                    MaterialAlertDialogBuilder(activity)
                                        .setTitle(R.string.template_delete_maps_rule)
                                        .setMessage(rules[position])
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            template.mapsRules.remove(rules[position])
                                            activity.runOnUiThread {
                                                adapter.remove(rules[position])
                                                setListCount(template.mapsRules.size)
                                            }
                                        }.show()
                                    true
                                }
                            }
                            findViewById<Button>(R.id.add_new_rule).setOnClickListener {
                                val editText = findViewById<EditText>(R.id.et_new_rule)
                                val newRule = editText.text.toString()
                                editText.text.clear()
                                if (newRule.isEmpty() || template.mapsRules.contains(newRule)) return@setOnClickListener
                                template.mapsRules.add(newRule)
                                activity.runOnUiThread {
                                    adapter.add(newRule)
                                    setListCount(template.mapsRules.size)
                                }
                            }
                        })
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }
}