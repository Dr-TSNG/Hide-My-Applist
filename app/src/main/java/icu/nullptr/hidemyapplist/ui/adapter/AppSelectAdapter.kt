package icu.nullptr.hidemyapplist.ui.adapter

import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.view.AppItemView
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

abstract class AppSelectAdapter(
    private val firstFilter: ((String) -> Boolean)? = null
) : RecyclerView.Adapter<AppSelectAdapter.ViewHolder>(), Filterable {

    abstract class ViewHolder(view: AppItemView) : RecyclerView.ViewHolder(view) {
        abstract fun bind(packageName: String)
    }

    private inner class AppFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            return runBlocking {
                val constraintLowered = constraint.toString().lowercase()
                val filteredList = PackageHelper.appList.first().filter {
                    if (firstFilter?.invoke(it) == false) return@filter false
                    if (!PrefManager.appFilter_showSystem && PackageHelper.isSystem(it)) return@filter false
                    val label = PackageHelper.loadAppLabel(it)
                    val packageInfo = PackageHelper.loadPackageInfo(it)
                    label.lowercase().contains(constraintLowered) || packageInfo.packageName.lowercase().contains(constraintLowered)
                }

                FilterResults().also { it.values = filteredList }
            }
        }

        @Suppress("UNCHECKED_CAST", "NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            filteredList = results.values as List<String>
            notifyDataSetChanged()
        }
    }

    private val mFilter = AppFilter()

    protected var filteredList: List<String> = listOf()

    override fun getItemCount() = filteredList.size

    override fun getItemId(position: Int) = filteredList[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(filteredList[position])

    override fun getFilter(): Filter = mFilter
}
