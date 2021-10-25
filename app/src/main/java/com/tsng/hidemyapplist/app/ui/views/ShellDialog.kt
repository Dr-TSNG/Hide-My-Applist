package com.tsng.hidemyapplist.app.ui.views

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.ShellDialogBinding


class ShellDialog(private val context: Context) {

    private val binding = ShellDialogBinding.inflate(LayoutInflater.from(context))
    private val console = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            binding.console.append("$s\n")
        }
    }

    private var commands: Array<out String>? = null
    private var successfulMainButtonText = context.getText(android.R.string.ok)
    private var successfulViceButtonText: CharSequence? = null
    private var failedButtonText = context.getText(android.R.string.ok)
    private var successfulMainButtonClickListener: View.OnClickListener? = null
    private var successfulViceButtonClickListener: View.OnClickListener? = null
    private var failedButtonClickListener: View.OnClickListener? = null

    fun setCommands(vararg cmd: String): ShellDialog {
        commands = cmd
        return this
    }

    fun setSuccessfulMainButton(@StringRes ResId: Int?, onClickListener: View.OnClickListener?): ShellDialog {
        ResId?.let { successfulMainButtonText = context.getText(it) }
        successfulMainButtonClickListener = onClickListener
        return this
    }

    fun setSuccessfulViceButton(@StringRes ResId: Int?, onClickListener: View.OnClickListener?): ShellDialog {
        successfulViceButtonText = context.getText(ResId ?: android.R.string.cancel)
        successfulViceButtonClickListener = onClickListener
        return this
    }

    fun setFailedButton(@StringRes ResId: Int?, onClickListener: View.OnClickListener?): ShellDialog {
        ResId?.let { failedButtonText = context.getText(it) }
        failedButtonClickListener = onClickListener
        return this
    }

    fun create() {
        commands ?: throw IllegalArgumentException("Commands not set")

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.shell)
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Stub!", null)
            .setNegativeButton("Stub!", null)
            .create()
            .apply {
                create()
                getButton(Dialog.BUTTON_POSITIVE).visibility = View.GONE
                getButton(Dialog.BUTTON_NEGATIVE).visibility = View.GONE
                show()
            }

        if (!Shell.getShell().isRoot) {
            console.add(context.getString(R.string.no_root_permission))
            dialog.getButton(Dialog.BUTTON_POSITIVE).apply {
                visibility = View.VISIBLE
                text = failedButtonText
                setOnClickListener { failedButtonClickListener; dialog.dismiss() }
            }
            return
        }

        Shell.su(*commands!!).to(console, console).submit { out: Shell.Result ->
            console.add("------------")
            console.add("result code: ${out.code}")
            if (out.isSuccess) {
                console.add(context.getString(R.string.execute_successfully))
                dialog.getButton(Dialog.BUTTON_POSITIVE).apply {
                    visibility = View.VISIBLE
                    text = successfulMainButtonText
                    setOnClickListener { successfulMainButtonClickListener?.onClick(it); dialog.dismiss() }
                }
                if (successfulViceButtonText != null)
                    dialog.getButton(Dialog.BUTTON_NEGATIVE).apply {
                        visibility = View.VISIBLE
                        text = successfulViceButtonText
                        setOnClickListener { successfulViceButtonClickListener?.onClick(it); dialog.dismiss() }
                    }
            } else {
                console.add(context.getString(R.string.execute_failed))
                dialog.getButton(Dialog.BUTTON_POSITIVE).apply {
                    visibility = View.VISIBLE
                    text = failedButtonText
                    setOnClickListener { failedButtonClickListener?.onClick(it); dialog.dismiss() }
                }
            }
        }
    }
}