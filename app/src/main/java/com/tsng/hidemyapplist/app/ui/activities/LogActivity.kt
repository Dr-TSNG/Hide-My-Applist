package com.tsng.hidemyapplist.app.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.helpers.ServiceHelper
import com.tsng.hidemyapplist.databinding.FragmentLogRawBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : AppCompatActivity() {
    var rawLogs: String? = null
    var showRawLogs: Boolean = true

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_refresh -> {
                rawLogs = ServiceHelper.getLogs()

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
                    val uri = FileProvider.getUriForFile(
                        this@LogActivity,
                        "com.tsng.hidemyapplist.fileprovider",
                        tmp
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
                startActivity(Intent.createChooser(intent, title))
                true
            }
            R.id.toolbar_clean -> {
                ServiceHelper.cleanLogs()
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
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        rawLogs = ServiceHelper.getLogs()
        replaceFragment(RawFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.log_container, fragment)
            .commit()
    }

    class RawFragment : Fragment() {
        private lateinit var binding: FragmentLogRawBinding

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentLogRawBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            binding.rawLogText.text = (requireActivity() as LogActivity).rawLogs
        }
    }
}