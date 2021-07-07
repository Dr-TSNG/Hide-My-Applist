package com.tsng.hidemyapplist.app.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.tsng.hidemyapplist.R

class SelectListView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    private val textView: TextView
    private val button: Button
    private val rawText: CharSequence

    init {
        inflate(context, R.layout.view_select_list, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SelectListView)
        findViewById<ImageView>(R.id.list_image).setImageDrawable(attributes.getDrawable(R.styleable.SelectListView_listDrawable))
        textView = findViewById<TextView>(R.id.list_text).apply {
            rawText = attributes.getText(R.styleable.SelectListView_listText)
            text = rawText
        }
        button = findViewById(R.id.list_button)
        if (!attributes.getBoolean(R.styleable.SelectListView_showButton, true))
            button.visibility = View.INVISIBLE
        attributes.recycle()
    }

    fun setListCount(cnt: Int) {
        textView.text = rawText.replaceFirst(Regex("#"), cnt.toString())
    }

    fun setOnButtonClickListener(l: OnClickListener?) {
        button.setOnClickListener(l)
    }
}