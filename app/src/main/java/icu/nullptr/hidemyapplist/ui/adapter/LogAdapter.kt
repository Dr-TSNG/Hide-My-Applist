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
import icu.nullptr.hidemyapplist.ui.util.themeColor
import java.util.regex.Pattern

class LogAdapter(context: Context) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    private val pattern = Pattern.compile("\\[ ?(.*)] (.*) \\((.*)\\) (.*)", Pattern.DOTALL)
    private val colorDebug = context.getColor(R.color.debug)
    private val colorInfo = context.getColor(R.color.info)
    private val colorWarn = context.getColor(R.color.warn)
    private val colorError =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) Color.RED
        else context.themeColor(android.R.attr.colorError)

    var logs = listOf<String>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(private val binding: LogItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            val matcher = pattern.matcher(text)
            matcher.find()
            val level = matcher.group(1)
            val date = matcher.group(2)
            val tag = matcher.group(3)
            val message = matcher.group(4)
            val color = when (level) {
                "DEBUG" -> colorDebug
                "INFO" -> colorInfo
                "WARN" -> colorWarn
                "ERROR" -> colorError
                else -> throw IllegalArgumentException("Unknown level: $level")
            }

            binding.level.setBackgroundColor(color)
            binding.level.text = level.substring(0, 1)
            binding.date.text = date
            binding.tag.text = tag
            binding.message.text = message
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
