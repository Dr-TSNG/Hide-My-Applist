package com.tsng.hidemyapplist.app.ui.views

import android.app.Activity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R

object FilterRulesView {
    @JvmStatic
    fun show(activity: Activity, ruleSet: MutableSet<String>, updateUi: (() -> Unit)? = null) {
        val rules = ruleSet.toMutableList()
        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, rules)
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.template_add_filter_rules)
            .setView(
                View.inflate(
                    activity,
                    R.layout.alert_customize_filter_rules,
                    null
                ).apply {
                    findViewById<ListView>(R.id.rule_list).apply {
                        this.adapter = adapter
                        setOnItemLongClickListener { _, _, position, _ ->
                            MaterialAlertDialogBuilder(activity)
                                .setTitle(R.string.template_delete_filter_rule)
                                .setMessage(rules[position])
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    ruleSet.remove(rules[position])
                                    activity.runOnUiThread {
                                        adapter.remove(rules[position])
                                        updateUi?.let { it() }
                                    }
                                }.show()
                            true
                        }
                    }
                    findViewById<Button>(R.id.add_new_rule).setOnClickListener {
                        val editText =
                            findViewById<EditText>(R.id.et_new_rule)
                        val newRule = editText.text.toString()
                        editText.text.clear()
                        if (newRule.isEmpty() || ruleSet.contains(newRule)) return@setOnClickListener
                        ruleSet.add(newRule)
                        activity.runOnUiThread {
                            adapter.add(newRule)
                            updateUi?.let { it() }
                        }
                    }
                })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}