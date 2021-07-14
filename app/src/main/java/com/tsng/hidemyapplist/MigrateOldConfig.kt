package com.tsng.hidemyapplist

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.app.JsonConfigManager
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.deepCopy
import com.tsng.hidemyapplist.app.makeToast

object MigrateOldConfig {
    @JvmStatic
    private fun migrateFromPre49() {
        val backup = Pair(globalConfig.templates.deepCopy(), globalConfig.scope.deepCopy())
        try {
            JsonConfigManager.edit {
                val oldTemplates = appContext.getSharedPreferences("Templates", MODE_PRIVATE)
                    ?.getStringSet("List", setOf()) ?: setOf()
                val oldScope = appContext.getSharedPreferences("Scope", MODE_PRIVATE)
                    ?.all as Map<String, String>? ?: mapOf()

                for (templateName in oldTemplates) {
                    val oldTemplate =
                        appContext.getSharedPreferences("tpl_$templateName", MODE_PRIVATE)
                            ?.all as Map<String, *>? ?: continue
                    val template = JsonConfig.Template(
                        isWhitelist = oldTemplate["WhiteList"] as Boolean,
                        appList = oldTemplate["HideApps"] as MutableSet<String>,
                        mapsRules = oldTemplate["MapsRules"] as MutableSet<String>
                    )
                    templates[templateName] = template

                    for ((packageName, applyTemplate) in oldScope)
                        if (applyTemplate == templateName) {
                            scope[packageName] = JsonConfig.AppConfig(
                                useWhitelist = template.isWhitelist,
                                enableAllHooks = oldTemplate["EnableAllHooks"] as Boolean,
                                excludeSystemApps = oldTemplate["ExcludeSystemApps"] as Boolean,
                                applyHooks = oldTemplate["ApplyHooks"] as MutableSet<String>,
                                applyTemplates = mutableSetOf(templateName)
                            )
                        }
                }
            }
        } catch (e: Exception) {
            JsonConfigManager.edit {
                clear()
                templates.putAll(backup.first)
                scope.putAll(backup.second)
            }
            throw RuntimeException("Error occurred when migrating config from pre 49 version")
                .apply { addSuppressed(e) }
        }
    }

    @JvmStatic
    fun doMigration(context: Context, version: Int) {
        if (version > 49) return
        MaterialAlertDialogBuilder(context)
            .setCancelable(false)
            .setTitle(R.string.migrate_config_title)
            .setMessage(R.string.migrate_config_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    when {
                        version <= 49 -> migrateFromPre49()
                    }
                    makeToast(R.string.migrate_config_successful)
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    makeToast(R.string.migrate_config_failed)
                    MaterialAlertDialogBuilder(context)
                        .setCancelable(false)
                        .setTitle(R.string.migrate_config_failed)
                        .setMessage(e.message)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.show_crash_log) { _, _ ->
                            MaterialAlertDialogBuilder(context)
                                .setCancelable(false)
                                .setTitle(R.string.migrate_config_failed)
                                .setMessage(e.stackTraceToString())
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                        .show()
                }
            }
            .show()
    }
}