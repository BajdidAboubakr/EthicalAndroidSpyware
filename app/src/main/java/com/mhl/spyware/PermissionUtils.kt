package com.mhl.spyware

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtils {
    companion object {
        const val PERMISSION_REQUEST_ALL = 100

        val CONTACTS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS
        )

        // Notification permissions for Android 13+ (TIRAMISU)
        val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf()
        }

        val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS
        )

        val CALL_LOG_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CALL_LOG
        )

        // Location permissions with Android 14 support
        val FOREGROUND_LOCATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val BACKGROUND_LOCATION_PERMISSION = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        // Audio permissions with Android 14 support
        val AUDIO_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO
            )
        }

        val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        fun arePermissionsGranted(activity: Activity, permissions: Array<String>): Boolean {
            if (permissions.isEmpty()) return true

            // Filter out permissions that don't exist on the current Android version
            val applicablePermissions = permissions.filter { permission ->
                try {
                    ContextCompat.checkSelfPermission(activity, permission)
                    true
                } catch (e: Exception) {
                    Log.d("Permissions", "Permission $permission not available on this Android version")
                    false
                }
            }.toTypedArray()

            applicablePermissions.forEach { permission ->
                val isGranted = ContextCompat.checkSelfPermission(
                    activity,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
                Log.d("Permissions", "Checking $permission: $isGranted")
            }

            return applicablePermissions.all {
                ContextCompat.checkSelfPermission(
                    activity,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun shouldShowRequestPermissionRationale(activity: Activity, permissions: Array<String>): Boolean {
            // Filter out permissions that don't exist on the current Android version
            val applicablePermissions = permissions.filter { permission ->
                try {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            return applicablePermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
        }

        fun checkContactsPermission(activity: Activity) =
            arePermissionsGranted(activity, CONTACTS_PERMISSIONS)

        fun checkSMSPermission(activity: Activity) =
            arePermissionsGranted(activity, SMS_PERMISSIONS)

        fun checkCallLogPermission(activity: Activity) =
            arePermissionsGranted(activity, CALL_LOG_PERMISSIONS)

        fun checkForegroundLocationPermission(activity: Activity) =
            arePermissionsGranted(activity, FOREGROUND_LOCATION_PERMISSIONS)

        fun checkBackgroundLocationPermission(activity: Activity) =
            arePermissionsGranted(activity, BACKGROUND_LOCATION_PERMISSION)

        fun checkAudioPermission(activity: Activity) =
            arePermissionsGranted(activity, AUDIO_PERMISSIONS)

        fun checkCameraPermission(activity: Activity) =
            arePermissionsGranted(activity, CAMERA_PERMISSIONS)

        fun checkNotificationPermission(activity: Activity) =
            arePermissionsGranted(activity, NOTIFICATION_PERMISSIONS)
    }
}
