package icu.nullptr.hidemyapplist.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.view.AppItemView
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSelectViewModel
import icu.nullptr.hidemyapplist.util.PackageHelper

class AppAdapter(
    private val viewModel: AppSelectViewModel
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    var onClickListener: ((String) -> Unit)? = null

    inner class ViewHolder(view: AppItemView) : RecyclerView.ViewHolder(view) {

        init {
            view.setOnClickListener {
                with(viewModel) {
                    val packageName = list[absoluteAdapterPosition]
                    onClickListener?.invoke(packageName)
                    if (isMultiSelect) {
                        packageName.isChecked = !packageName.isChecked
                        view.isChecked = packageName.isChecked
                    }
                }
            }
        }

        fun bind(packageName: String) {
            with(viewModel) {
                (itemView as AppItemView).let {
                    it.load(packageName)
                    if (isMultiSelect) it.isChecked = packageName.isChecked
                    else it.showEnabled = packageName.isChecked
                    if (!PrefManager.filter_showSystem && PackageHelper.isSystem(packageName)) {
                        it.visibility = ViewGroup.GONE
                        it.layoutParams.height = 0
                    } else {
                        it.visibility = ViewGroup.VISIBLE
                        it.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppItemView(parent.context, viewModel.isMultiSelect)
        view.showEnabled = !viewModel.isMultiSelect
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun getItemCount() = viewModel.list.size

    override fun getItemId(position: Int) = viewModel.list[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(viewModel.list[position])
}
