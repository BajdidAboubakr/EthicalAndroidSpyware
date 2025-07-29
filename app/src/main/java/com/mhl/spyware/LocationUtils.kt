package com.mhl.spyware

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*


class LocationUtils(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null




    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(onLocationReceived: (Location?) -> Unit) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    onLocationReceived(location)
                }
                .addOnFailureListener { e ->
                    Log.e("LocationUtils", "Error getting last known location", e)
                    onLocationReceived(null)
                }
        } catch (e: SecurityException) {
            Log.e("LocationUtils", "Location permission not granted", e)
            onLocationReceived(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(
        interval: Long = 10000L,
        fastestInterval: Long = 5000L,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
        onLocationReceived: (Location?) -> Unit
    ) {
        if (!isLocationEnabled(context)) {
            Log.e("LocationUtils", "Location services are disabled.")
            onLocationReceived(null)
            return
        }

        if (!hasLocationPermission(context)) {
            Log.e("LocationUtils", "Location permission not granted.")
            onLocationReceived(null)
            return
        }

        val locationRequest = LocationRequest.Builder(interval)
            .setPriority(priority)
            .setMinUpdateIntervalMillis(fastestInterval)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.d("LocationUtils", "Location update received: ${location.latitude}, ${location.longitude}")
                    onLocationReceived(location)
                } else {
                    Log.e("LocationUtils", "No location updates available.")
                    onLocationReceived(null)
                }
            }
        }

        locationCallback?.let { callback ->
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            Log.d("LocationUtils", "Started requesting location updates.")
        }
    }



    private fun hasLocationPermission(context: Context): Boolean {
        return android.Manifest.permission.ACCESS_FINE_LOCATION.let {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it)
        } == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }


}
