package com.tsng.hidemyapplist.app.helpers

import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.IHMAService

object ServiceHelper {

    private const val TAG = "ServiceHelper"

    private fun getService(): IHMAService? {
        val pm = ServiceManager.getService("package")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(Constants.DESCRIPTOR)
            data.writeInt(Constants.ACTION_GET_BINDER)
            pm.transact(Constants.TRANSACTION, data, reply, 0)
            reply.readException()
            val binder = reply.readStrongBinder()
            IHMAService.Stub.asInterface(binder)
        } catch (e: RemoteException) {
            Log.d(TAG, "Failed to get binder")
            null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun getServiceVersion() = getService()?.serviceVersion ?: 0

    fun getServeTimes() = getService()?.filterCount ?: 0

    fun getLogs() = getService()?.logs

    fun cleanLogs() {
        getService()?.clearLogs()
    }

    fun submitConfig(json: String) {
        getService()?.syncConfig(json)
    }

    fun stopSystemService(cleanEnv: Boolean) {
        getService()?.stopService(cleanEnv)
    }
}
