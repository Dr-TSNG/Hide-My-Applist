package icu.nullptr.hidemyapplist.ui.adapter

import android.view.ViewGroup
import icu.nullptr.hidemyapplist.ui.view.AppItemView

class AppScopeAdapter(
    private val checked: MutableSet<String>,
    firstFilter: ((String) -> Boolean)?,
) : AppSelectAdapter(firstFilter) {

    private inline var String.isChecked
        get() = checked.contains(this)
        set(value) {
            if (value) checked.add(this) else checked.remove(this)
        }

    inner class ViewHolder(view: AppItemView) : AppSelectAdapter.ViewHolder(view) {
        init {
            view.setOnClickListener {
                val packageName = filteredList[absoluteAdapterPosition]
                packageName.isChecked = !packageName.isChecked
                view.isChecked = packageName.isChecked
            }
        }

        override fun bind(packageName: String) {
            (itemView as AppItemView).let {
                it.load(packageName)
                it.isChecked = packageName.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, true)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }
}
