package icu.nullptr.hidemyapplist.xposed

import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.text.SimpleDateFormat
import java.util.*

private fun parseLog(level: Int, tag: String, msg: String, cause: Throwable? = null) = buildString {
    val levelStr = when (level) {
        Log.DEBUG -> "DEBUG"
        Log.INFO -> " INFO"
        Log.WARN -> " WARN"
        Log.ERROR -> "ERROR"
        else -> "?????"
    }
    val date = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    append("[$levelStr] $date ($tag) $msg")
    if (!endsWith('\n')) append('\n')
    if (cause != null) append(Log.getStackTraceString(cause))
    if (!endsWith('\n')) append('\n')
}

private fun log(level: Int, tag: String, msg: String, cause: Throwable? = null) {
    if (level <= Log.DEBUG && HMAService.instance?.config?.detailLog == false) return
    val parsedLog = parseLog(level, tag, msg, cause)
    HMAService.instance?.addLog(parsedLog)
    XposedBridge.log(parsedLog)
}

fun logD(tag: String, msg: String, cause: Throwable? = null) = log(Log.DEBUG, tag, msg, cause)
fun logI(tag: String, msg: String, cause: Throwable? = null) = log(Log.INFO, tag, msg, cause)
fun logW(tag: String, msg: String, cause: Throwable? = null) = log(Log.WARN, tag, msg, cause)
fun logE(tag: String, msg: String, cause: Throwable? = null) = log(Log.ERROR, tag, msg, cause)
