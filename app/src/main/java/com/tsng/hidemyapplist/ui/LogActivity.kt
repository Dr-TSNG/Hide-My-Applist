package com.tsng.hidemyapplist.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.xposed.XposedUtils
import kotlinx.android.synthetic.main.fragment_log_raw.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : AppCompatActivity() {
    /* ----- NOT COMPLETED YET ----- */
    open class MLog(val level: Int, val tool: Int) {
        /* level: D/I/E tool: Xposed/Native */
        companion object {
            const val DEBUG = 0
            const val INFO = 1
            const val ERROR = 2
            const val XPOSED = 10
            const val NATIVE = 11
            const val SERVICE_INFO = 100
            const val INTERCEPTION = 101
        }
    }

    class ServiceLog(level: Int, val message: String) : MLog(level, XPOSED)

    open class InterceptionLog(
            level: Int,
            tool: Int,
            val caller: String,
            val method: String
    ) : MLog(level, tool)

    class PKMSLog(level: Int, caller: String, method: String, val requests: Set<String>) : InterceptionLog(level, XPOSED, caller, method)

    class FileLog(level: Int, tool: Int, caller: String, method: String, val request: String) : InterceptionLog(level, tool, caller, method)

    class LogAdapter(val logList: List<MLog>)
        : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val logLevel: TextView = view.findViewById(R.id.log_level)
            val logTool: TextView = view.findViewById(R.id.log_tool)
        }

        override fun getItemViewType(position: Int): Int {
            return when (logList[position].javaClass) {
                ServiceLog::javaClass -> MLog.SERVICE_INFO
                else -> -1
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layout = when (viewType) {
                MLog.SERVICE_INFO -> R.layout.log_service
                MLog.INTERCEPTION -> R.layout.log_interception
                else -> null
            }
            val view = LayoutInflater.from(parent.context)
                    .inflate(layout!!, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val log = logList[position]
            holder.logLevel.text = when (log.level) {
                MLog.DEBUG -> "D"
                MLog.INFO -> "I"
                MLog.ERROR -> "E"
                else -> null
            }
            holder.logTool.text = when (log.tool) {
                MLog.XPOSED -> "Xposed"
                MLog.NATIVE -> "Native"
                else -> null
            }
        }

        override fun getItemCount(): Int {
            return logList.size
        }
    }
    /* ----------------------------- */

    var rawLogs: String? = null
    var showRawLogs: Boolean = true

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_logs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_refresh -> {
                rawLogs = XposedUtils.getLogs(this)

                replaceFragment(RawFragment())
                // replaceFragment(if (showRawLogs) BuiltifulFragment() else RawFragment())
                true
            }
            R.id.toolbar_export -> {
                val date = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                File(cacheDir.path + "/logs").mkdirs()
                val tmp = File(cacheDir.path + "/logs/hma_logs_$date.log")
                rawLogs?.let { tmp.writeText(it) }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/*"
                    val uri = FileProvider.getUriForFile(this@LogActivity, "com.tsng.hidemyapplist.fileprovider", tmp)
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
                startActivity(Intent.createChooser(intent, title))
                true
            }
            R.id.toolbar_clean -> {
                XposedUtils.cleanLogs(this)
                rawLogs = null

                replaceFragment(RawFragment())
                // replaceFragment(if (showRawLogs) BuiltifulFragment() else RawFragment())
                true
            }
            /*
            R.id.toolbar_raw_logs -> {
                item.isChecked = !item.isChecked
                showRawLogs = item.isChecked
                replaceFragment(if (showRawLogs) BuiltifulFragment() else RawFragment())
                true
            }
            */
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        rawLogs = XposedUtils.getLogs(this)
        replaceFragment(RawFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.log_container, fragment)
                .commit()
    }

    class RawFragment : Fragment() {
        private lateinit var activity: LogActivity

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_log_raw, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            activity = requireActivity() as LogActivity
            log_tv_raw.text = activity.rawLogs
        }
    }
}