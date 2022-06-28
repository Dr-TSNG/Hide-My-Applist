package icu.nullptr.hidemyapplist.xposed

import android.content.pm.IPackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.hidemyapplist.common.Constants

object BridgeService {

    private const val TAG = "HMA-Bridge"

    @JvmStatic
    private lateinit var service: HMAService
    private var appUid = 0

    fun start(pms: IPackageManager) {
        service = HMAService(pms)
        appUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            service.pms.getPackageUid(Constants.APP_PACKAGE_NAME, 0L, 0)
        } else {
            service.pms.getPackageUid(Constants.APP_PACKAGE_NAME, 0, 0)
        }
        doHooks()
    }

    private fun doHooks() {
        Binder::class.java.method("execTransact").hookBefore { param ->
            val code = param.args[0] as Int
            val dataObj = param.args[1] as Long
            val replyObj = param.args[2] as Long
            val flags = param.args[3] as Int
            if (code == Constants.TRANSACTION) {
                param.result = execTransact(code, dataObj, replyObj, flags)
            }
        }
    }

    private fun execTransact(code: Int, dataObj: Long, replyObj: Long, flags: Int): Boolean {
        val fromNativePointerMethod by lazy {
            Parcel::class.java.method("obtain", argTypes = argTypes(Long::class.java))
        }

        val data = fromNativePointerMethod.invoke(null, dataObj) as Parcel? ?: return false
        val reply = fromNativePointerMethod.invoke(null, replyObj) as Parcel?

        val res = try {
            onTransact(code, data, reply, flags)
        } catch (e: Exception) {
            if (flags and IBinder.FLAG_ONEWAY != 0) {
                Log.w(TAG, "Caught an exception from the binder stub implementation.")
            } else if (reply != null) {
                reply.setDataPosition(0)
                reply.writeException(e)
            }
            false
        } finally {
            data.setDataPosition(0)
            reply?.setDataPosition(0)
        }
        if (res) {
            data.recycle()
            reply?.recycle()
        }
        return res
    }

    private fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        data.enforceInterface(Constants.DESCRIPTOR)
        return when (data.readInt()) {
            Constants.ACTION_GET_BINDER -> {
                if (Binder.getCallingUid() == appUid && service.hooksInstalled) {
                    reply?.writeNoException()
                    reply?.writeStrongBinder(service.asBinder())
                    true
                } else {
                    Log.w(TAG, "Invalid connection")
                    false
                }
            }
            Constants.ACTION_SEND_LOG -> {
                service.sendLog(data.readInt(), data.readString()!!, data.readString()!!)
                false
            }
            else -> false
        }
    }
}
