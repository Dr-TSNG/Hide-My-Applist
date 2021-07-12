package com.tsng.hidemyapplist.app.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tsng.hidemyapplist.R

class OptionView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    init {
        context?.let {
            inflate(context, R.layout.view_option, this)
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.OptionView)
            findViewById<ImageView>(R.id.option_image).setImageDrawable(attributes.getDrawable(R.styleable.OptionView_optionDrawable))
            findViewById<TextView>(R.id.option_text).text = attributes.getText(R.styleable.OptionView_optionText)
            attributes.recycle()
        }
    }
}