package icu.nullptr.hidemyapplist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.ListItemViewBinding


class ListItemView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ListItemView, defStyleAttr, 0)
        val icon = typedArray.getResourceId(R.styleable.ListItemView_icon, 0)
        val text = typedArray.getString(R.styleable.ListItemView_text)
        typedArray.recycle()
        val binding = ListItemViewBinding.inflate(LayoutInflater.from(context), this, true)
        binding.icon.setImageResource(icon)
        binding.text.text = text
    }
}
