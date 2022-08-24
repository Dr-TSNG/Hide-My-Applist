package icu.nullptr.hidemyapplist.ui.util

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.google.android.material.color.DynamicColors
import com.tsng.hidemyapplist.R
import icu.nullptr.hidemyapplist.service.PrefManager
import rikka.core.util.ResourceUtils

object ThemeUtils {

    private val colorThemeMap = mapOf(
        "SAKURA" to R.style.ThemeOverlay_MaterialSakura,
        "MATERIAL_RED" to R.style.ThemeOverlay_MaterialRed,
        "MATERIAL_PINK" to R.style.ThemeOverlay_MaterialPink,
        "MATERIAL_PURPLE" to R.style.ThemeOverlay_MaterialPurple,
        "MATERIAL_DEEP_PURPLE" to R.style.ThemeOverlay_MaterialDeepPurple,
        "MATERIAL_INDIGO" to R.style.ThemeOverlay_MaterialIndigo,
        "MATERIAL_BLUE" to R.style.ThemeOverlay_MaterialBlue,
        "MATERIAL_LIGHT_BLUE" to R.style.ThemeOverlay_MaterialLightBlue,
        "MATERIAL_CYAN" to R.style.ThemeOverlay_MaterialCyan,
        "MATERIAL_TEAL" to R.style.ThemeOverlay_MaterialTeal,
        "MATERIAL_GREEN" to R.style.ThemeOverlay_MaterialGreen,
        "MATERIAL_LIGHT_GREEN" to R.style.ThemeOverlay_MaterialLightGreen,
        "MATERIAL_LIME" to R.style.ThemeOverlay_MaterialLime,
        "MATERIAL_YELLOW" to R.style.ThemeOverlay_MaterialYellow,
        "MATERIAL_AMBER" to R.style.ThemeOverlay_MaterialAmber,
        "MATERIAL_ORANGE" to R.style.ThemeOverlay_MaterialOrange,
        "MATERIAL_DEEP_ORANGE" to R.style.ThemeOverlay_MaterialDeepOrange,
        "MATERIAL_BROWN" to R.style.ThemeOverlay_MaterialBrown,
        "MATERIAL_BLUE_GREY" to R.style.ThemeOverlay_MaterialBlueGrey
    )

    private const val THEME_DEFAULT = "DEFAULT"
    private const val THEME_BLACK = "BLACK"

    val isSystemAccent get() = DynamicColors.isDynamicColorAvailable() && PrefManager.followSystemAccent

    fun getNightTheme(context: Context): String {
        return if (PrefManager.blackDarkTheme && ResourceUtils.isNightMode(context.resources.configuration))
            THEME_BLACK else THEME_DEFAULT
    }

    @StyleRes
    fun getNightThemeStyleRes(context: Context): Int {
        return if (PrefManager.blackDarkTheme && ResourceUtils.isNightMode(context.resources.configuration))
            R.style.ThemeOverlay_Black else R.style.ThemeOverlay
    }

    val colorTheme get() = if (isSystemAccent) "SYSTEM" else PrefManager.themeColor

    val colorThemeStyleRes: Int
        @StyleRes get() = colorThemeMap[colorTheme] ?: R.style.ThemeOverlay_MaterialBlue

    /**
     * Retrieve a color from the current [android.content.res.Resources.Theme].
     */
    @ColorInt
    fun Context.themeColor(
        @AttrRes themeAttrId: Int
    ): Int {
        val style = obtainStyledAttributes(intArrayOf(themeAttrId))
        val color = style.getColor(0, Color.MAGENTA)
        style.recycle()
        return color
    }

    @ColorInt
    fun Fragment.themeColor(
        @AttrRes themeAttrId: Int
    ) = requireContext().themeColor(themeAttrId)

    @ColorInt
    fun Fragment.getColor(
        @ColorRes colorId: Int
    ) = requireContext().getColor(colorId)
}
