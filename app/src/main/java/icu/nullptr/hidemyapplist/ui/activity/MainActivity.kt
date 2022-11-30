package icu.nullptr.hidemyapplist.ui.activity

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.material.color.DynamicColors
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.ActivityMainBinding
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils
import rikka.material.app.MaterialActivity

class MainActivity : MaterialActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ThemeUtils.isSystemAccent) DynamicColors.applyToActivityIfAvailable(this)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        setupWithNavController(binding.bottomNav, navController)

        MobileAds.initialize(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        if (!ThemeUtils.isSystemAccent) theme.applyStyle(ThemeUtils.colorThemeStyleRes, true)
        theme.applyStyle(ThemeUtils.getNightThemeStyleRes(this), true)
    }

    override fun computeUserThemeKey() = ThemeUtils.colorTheme + ThemeUtils.getNightThemeStyleRes(this)

    override fun onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars()
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
}
