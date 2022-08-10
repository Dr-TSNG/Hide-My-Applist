package icu.nullptr.hidemyapplist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.databinding.AppItemViewBinding
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding by viewBinding<AppItemViewBinding>(createMethod = CreateMethod.INFLATE)

    var showEnabled: Boolean
        get() = binding.enabled.visibility == VISIBLE
        set(value) {
            binding.enabled.visibility = if (value) VISIBLE else GONE
        }

    var isChecked: Boolean
        get() = binding.checkbox.isChecked
        set(value) {
            binding.checkbox.isChecked = value
        }

    constructor(context: Context, isCheckable: Boolean) : this(context) {
        binding.checkbox.visibility = if (isCheckable) VISIBLE else GONE
    }

    fun load(packageName: String) {
        binding.packageName.text = packageName
        binding.label.text = PackageHelper.loadAppLabel(packageName)
        binding.icon.setImageBitmap(PackageHelper.loadAppIcon(packageName))
    }
}
