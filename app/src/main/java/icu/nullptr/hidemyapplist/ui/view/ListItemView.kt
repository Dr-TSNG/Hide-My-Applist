package icu.nullptr.hidemyapplist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.ListItemViewBinding

class ListItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding by viewBinding<ListItemViewBinding>(createMethod = CreateMethod.INFLATE)

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ListItemView, defStyleAttr, defStyleRes)
        val icon = typedArray.getResourceId(R.styleable.ListItemView_icon, 0)
        val text = typedArray.getString(R.styleable.ListItemView_text)
        val buttonText = typedArray.getText(R.styleable.ListItemView_buttonText)
        typedArray.recycle()
        binding.icon.setImageResource(icon)
        binding.text.text = text
        if (buttonText != null) {
            binding.button.visibility = VISIBLE
            binding.button.text = buttonText
        }
    }

    var text: CharSequence?
        get() = binding.text.text
        set(value) {
            binding.text.text = value
        }

    fun setIcon(@DrawableRes icon: Int) {
        binding.icon.setImageResource(icon)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        if (binding.button.visibility == VISIBLE) {
            binding.button.setOnClickListener(l)
        } else {
            super.setOnClickListener(l)
        }
    }
}
