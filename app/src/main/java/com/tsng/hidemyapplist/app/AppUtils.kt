package com.tsng.hidemyapplist.app

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.gson.Gson
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext

fun makeToast(@StringRes resId: Int) {
    Toast.makeText(appContext, resId, Toast.LENGTH_SHORT).show()
}

fun makeToast(text: CharSequence) {
    Toast.makeText(appContext, text, Toast.LENGTH_SHORT).show()
}

fun <T : Any> T.deepCopy(): T = Gson().fromJson(Gson().toJson(this), this::class.java)

fun AppCompatActivity.startFragment(fragment: Fragment, addToBackStack: Boolean = true) {
    val transaction = supportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    if (addToBackStack) transaction.addToBackStack(null)
    transaction.commit()
}

fun Fragment.startFragment(fragment: Fragment, addToBackStack: Boolean = true) {
    val transaction = parentFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    if (addToBackStack) transaction.addToBackStack(null)
    transaction.commit()
}