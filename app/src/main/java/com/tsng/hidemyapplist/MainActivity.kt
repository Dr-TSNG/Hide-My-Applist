package com.tsng.hidemyapplist

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.ui.AboutActivity
import com.tsng.hidemyapplist.ui.DetectionActivity
import com.tsng.hidemyapplist.ui.ScopeManageActivity
import com.tsng.hidemyapplist.ui.TemplateManageActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private fun getXposedStatus(): Int { return -1 }

    private fun isSelfHooked(): Boolean { return false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (getXposedStatus() != -1) {
            xposed_status.setCardBackgroundColor(getColor(R.color.teal))
            xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_activited))
            xposed_status_text.text = getString(R.string.xposed_activated)
            xposed_status_sub_text.text = when (getXposedStatus()) {
                0b00 -> getString(R.string.xposed_hook_mode_not_selected)
                0b01 -> getString(R.string.xposed_hook_mode_system)
                0b10 -> getString(R.string.xposed_hook_mode_individual)
                0b11 -> getString(R.string.xposed_hook_mode_mixed)
                else -> "Unknown hook mode"
            }
        } else {
            xposed_status.setCardBackgroundColor(getColor(R.color.gray))
            xposed_status_icon.setImageDrawable(getDrawable(R.drawable.ic_not_activated))
            xposed_status_text.text = getString(R.string.xposed_not_activated)
            xposed_status_sub_text.text = getString(R.string.xposed_hook_disabled)
        }
        makeUpdateAlert()
        menu_detection_test.setOnClickListener(this)
        menu_template_manage.setOnClickListener(this)
        menu_scope_manage.setOnClickListener(this)
        menu_about.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.menu_detection_test -> startActivity(Intent(this, DetectionActivity::class.java))
            R.id.menu_template_manage ->
                if (isSelfHooked())
                    Toast.makeText(this, getString(R.string.xposed_disable_hook_self_first), Toast.LENGTH_SHORT).show()
                else
                    startActivity(Intent(this, TemplateManageActivity::class.java))
            R.id.menu_scope_manage ->
                if (isSelfHooked())
                    Toast.makeText(this, getString(R.string.xposed_disable_hook_self_first), Toast.LENGTH_SHORT).show()
                else
                    startActivity(Intent(this, ScopeManageActivity::class.java))
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun makeUpdateAlert() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if (pref.getInt("LastVersion", 0) < BuildConfig.VERSION_CODE) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.updates)
                    .setMessage(R.string.updates_log)
                    .setPositiveButton(R.string.accept, null)
                    .show()
        }
    }
}