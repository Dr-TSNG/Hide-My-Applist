package com.tsng.hidemyapplist.app

import android.widget.Toast
import androidx.annotation.StringRes
import com.google.gson.Gson
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext

fun makeToast(@StringRes resId: Int) {
    Toast.makeText(appContext, resId, Toast.LENGTH_SHORT).show()
}

fun makeToast(text: CharSequence) {
    Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
}

fun <T : Any> T.deepCopy(): T = Gson().fromJson(Gson().toJson(this), this::class.java)