package icu.nullptr.hidemyapplist.ui.util

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Retrieve a color from the current [android.content.res.Resources.Theme].
 */
@ColorInt
fun Context.themeColor(
    @AttrRes themeAttrId: Int
) = obtainStyledAttributes(intArrayOf(themeAttrId)).use {
    it.getColor(0, Color.MAGENTA)
}
