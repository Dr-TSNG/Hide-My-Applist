package icu.nullptr.hidemyapplist.xposed

import android.content.pm.IPackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.method
import com.github.kyuubiran.ezxhelper.utils.staticMethod
import icu.nullptr.hidemyapplist.common.Constants

object BridgeService {

    private const val TAG = "HMA-Bridge"

    private var appUid = 0

    fun start(pms: IPackageManager) {
        logI(TAG, "Initialize HMAService")
        val service = HMAService(pms)
        appUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            service.pms.getPackageUid(Constants.APP_PACKAGE_NAME, 0L, 0)
        } else {
            service.pms.getPackageUid(Constants.APP_PACKAGE_NAME, 0, 0)
        }
        logD(TAG, "Client uid: $appUid")
        doHooks()
        logI(TAG, "Bridge service initialized")
    }

    private fun doHooks() {
        logD(TAG, "Hook binder transact")
        Binder::class.java.method(
            "execTransact",
            argTypes = argTypes(Int::class.java, Long::class.java, Long::class.java, Int::class.java)
        ).hookBefore { param ->
            val code = param.args[0] as Int
            val dataObj = param.args[1] as Long
            val replyObj = param.args[2] as Long
            val flags = param.args[3] as Int
            if (code == Constants.TRANSACTION) {
                param.result = execTransact(code, dataObj, replyObj, flags)
            }
        }
    }

    private val fromNativePointerMethod by lazy {
        Parcel::class.java.staticMethod("obtain", argTypes = argTypes(Long::class.java))
    }

    private fun execTransact(code: Int, dataObj: Long, replyObj: Long, flags: Int): Boolean {
        val data = fromNativePointerMethod.invoke(null, dataObj) as Parcel? ?: return false
        val reply = fromNativePointerMethod.invoke(null, replyObj) as Parcel?

        val res = try {
            onTransact(code, data, reply, flags)
        } catch (e: Exception) {
            if (flags and IBinder.FLAG_ONEWAY != 0) {
                logW(TAG, "Caught an exception from the binder stub implementation.")
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
                if (Binder.getCallingUid() == appUid && HMAService.instance != null) {
                    reply?.writeNoException()
                    reply?.writeStrongBinder(HMAService.instance!!.asBinder())
                    true
                } else {
                    logW(TAG, "Invalid connection")
                    false
                }
            }
            else -> false
        }
    }
}
