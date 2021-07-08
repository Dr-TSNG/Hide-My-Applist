package com.tsng.hidemyapplist.app.ui.adapters

import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.AppSelectList
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext

class AppSelectAdapter(private val appSelectList: AppSelectList) :
    RecyclerView.Adapter<AppSelectAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var isSystemApp = false
        lateinit var packageName: String

        val imageView: ImageView = view.findViewById(R.id.app_icon)
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val packageNameTextView: TextView = view.findViewById(R.id.app_package_name)
        val summaryTextView: TextView = view.findViewById(R.id.app_summary)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_app_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = appSelectList[position].first
        holder.isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        holder.packageName = appInfo.packageName

        holder.imageView.setImageDrawable(appInfo.loadIcon(appContext.packageManager))
        holder.appNameTextView.text = appInfo.loadLabel(appContext.packageManager)
        holder.packageNameTextView.text = appInfo.packageName
        if (holder.isSystemApp) {
            holder.summaryTextView.visibility = View.VISIBLE
            holder.summaryTextView.setText(R.string.system_app)
        } else holder.summaryTextView.visibility = View.INVISIBLE

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            appSelectList[position] = appInfo to isChecked
        }
        holder.checkBox.isChecked = appSelectList[position].second
    }

    override fun getItemCount() = appSelectList.size
}