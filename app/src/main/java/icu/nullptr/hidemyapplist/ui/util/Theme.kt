package icu.nullptr.hidemyapplist.ui.util

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment

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
