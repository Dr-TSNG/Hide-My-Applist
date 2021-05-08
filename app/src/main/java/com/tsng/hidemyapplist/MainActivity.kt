package com.tsng.hidemyapplist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.ui.*
import com.tsng.hidemyapplist.xposed.XposedUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private fun isModuleActivated(): Boolean {
        return false
    }

    private fun isHookSelf(): Boolean {
        return getSharedPreferences("Settings", MODE_PRIVATE).getBoolean("HookSelf", false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        startService(Intent(this, ProvidePreferenceService::class.java))
    }

    override fun onResume() {
        super.onResume()
        val serviceVersion = XposedUtils.getServiceVersion(this)
        if (isModuleActivated()) {
            if (serviceVersion != 0) {
                xposed_status.setCardBackgroundColor(getColor(R.color.teal))
                xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_activited))
                xposed_status_text.text = getString(R.string.xposed_activated)
            } else {
                xposed_status.setCardBackgroundColor(getColor(R.color.info))
                xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_service_not_running))
                xposed_status_text.text = getString(R.string.xposed_activated)
            }
        } else {
            xposed_status.setCardBackgroundColor(getColor(R.color.gray))
            xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_not_activated))
            xposed_status_text.text = getString(R.string.xposed_not_activated)
        }
        if (serviceVersion != 0) {
            if (serviceVersion != BuildConfig.VERSION_CODE) xposed_status_sub_text.text = getString(R.string.xposed_service_old)
            else xposed_status_sub_text.text = getString(R.string.xposed_service_on) + "$serviceVersion]"
            val text = getString(R.string.xposed_serve_times).split("#")
            xposed_status_serve_times.visibility = View.VISIBLE
            xposed_status_serve_times.text = text[0] + XposedUtils.getServeTimes(this) + text[2]
        }
        else {
            xposed_status_serve_times.visibility = View.GONE
            xposed_status_sub_text.text = getString(R.string.xposed_service_off)
        }
        makeUpdateAlert()
        menu_detection_test.setOnClickListener(this)
        menu_template_manage.setOnClickListener(this)
        menu_scope_manage.setOnClickListener(this)
        menu_settings.setOnClickListener(this)
        menu_about.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.menu_detection_test -> startActivity(Intent(this, DetectionActivity::class.java))
            R.id.menu_template_manage ->
                if (isHookSelf()) Toast.makeText(this, R.string.xposed_disable_hook_self_first, Toast.LENGTH_SHORT).show()
                else startActivity(Intent(this, TemplateManageActivity::class.java))
            R.id.menu_scope_manage ->
                if (isHookSelf()) Toast.makeText(this, R.string.xposed_disable_hook_self_first, Toast.LENGTH_SHORT).show()
                else startActivity(Intent(this, ScopeManageActivity::class.java))
            R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun makeUpdateAlert() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (pref.getInt("LastVersion", 0) < BuildConfig.VERSION_CODE) {
            pref.edit().putInt("LastVersion", BuildConfig.VERSION_CODE).apply()
            MaterialAlertDialogBuilder(this).setTitle(R.string.updates)
                    .setMessage(R.string.updates_log)
                    .setPositiveButton(R.string.accept, null)
                    .show()
        }
    }
}