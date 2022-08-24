package icu.nullptr.hidemyapplist.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.LogItemViewBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.themeColor
import java.util.regex.Pattern

class LogAdapter(context: Context) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class LogItem(
        val level: String,
        val date: String,
        val tag: String,
        val message: String
    )

    companion object {
        private val pattern = Pattern.compile("\\[ ?(.*)] (.*) \\((.*)\\) (.*)", Pattern.DOTALL)

        fun parseLog(text: String): LogItem? {
            val matcher = pattern.matcher(text)
            matcher.find()
            val level = matcher.group(1) ?: return null
            if (level == "DEBUG" && PrefManager.logFilter_level > 0 ||
                level == "INFO" && PrefManager.logFilter_level > 1 ||
                level == "WARN" && PrefManager.logFilter_level > 2
            ) return null
            val date = matcher.group(2) ?: return null
            val tag = matcher.group(3) ?: return null
            val message = matcher.group(4) ?: return null
            return LogItem(level, date, tag, message)
        }
    }

    private val colorDebug = context.getColor(R.color.debug)
    private val colorInfo = context.getColor(R.color.info)
    private val colorWarn = context.getColor(R.color.warn)
    private val colorError =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) Color.RED
        else context.themeColor(android.R.attr.colorError)

    var logs = listOf<LogItem>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(private val binding: LogItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(logItem: LogItem) {
            val color = when (logItem.level) {
                "DEBUG" -> colorDebug
                "INFO" -> colorInfo
                "WARN" -> colorWarn
                "ERROR" -> colorError
                else -> throw IllegalArgumentException("Unknown level: ${logItem.level}")
            }

            binding.level.setBackgroundColor(color)
            binding.level.text = logItem.level.substring(0, 1)
            binding.date.text = logItem.date
            binding.tag.text = logItem.tag
            binding.message.text = logItem.message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LogItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = logs.size

    override fun getItemId(position: Int) = logs[position].hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(logs[position])
}
