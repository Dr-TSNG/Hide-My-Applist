package com.tsng.hidemyapplist

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView = findViewById<BottomNavigationView>(R.id.nav_view)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(navView, navController)
        makeUpdateAlert()
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