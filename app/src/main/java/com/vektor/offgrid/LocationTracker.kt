package com.vektor.offgrid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// Define a callback interface for MainActivity to receive location updates
interface LocationUpdateListener {
    fun onLocationUpdated(location: Location?, magneticDeclination: Float)
    fun onGpsStatusChanged(status: String, accuracy: String)
    fun onRequestLocationPermissions() // Callback to Activity to request permissions
}

class LocationTracker(private val context: Context, private val listener: LocationUpdateListener) : LocationListener {

    private var useFusedLocationClient = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var playServicesLocationRequest: LocationRequest
    private lateinit var playServicesLocationCallback: LocationCallback

    private lateinit var locationManager: LocationManager
    // aospLocationListener is now 'this' because LocationTracker implements LocationListener
    // private lateinit var aospLocationListener: LocationListener // No longer needed as a separate field

    var currentLocation: Location? = null
        private set
    var magneticDeclination: Float = 0f
        private set

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (isGooglePlayServicesAvailable()) {
            useFusedLocationClient = true
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            createPlayServicesLocationRequest()
            createPlayServicesLocationCallback()
            listener.onGpsStatusChanged("Using Google Play Services Location...", "")
        } else {
            useFusedLocationClient = false
            // createAospLocationListener() // No longer needed as LocationTracker itself is the listener
            listener.onGpsStatusChanged("Using AOSP Location (No Google Play Services)...", "")
            Toast.makeText(context, "Google Play Services not found. Using AOSP Location.", Toast.LENGTH_LONG).show()
        }
    }

    fun checkLocationPermissionsAndStartUpdates() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }
            // No rationale needed for this abstraction, the Activity should handle it
            else -> {
                listener.onRequestLocationPermissions() // Delegate permission request to Activity
            }
        }
    }

    fun startLocationUpdates() {
        if (useFusedLocationClient) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(playServicesLocationRequest, playServicesLocationCallback, Looper.getMainLooper())
                listener.onGpsStatusChanged("Using Google Play Services Location...", "")
            } else {
                listener.onGpsStatusChanged("Location permission denied for FusedLocation.", "")
            }
        } else {
            try {
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (isGpsEnabled || isNetworkEnabled) {
                    if (isGpsEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            5000L, // minTime (ms)
                            10f,   // minDistance (meters)
                            this, // <--- Use 'this' as LocationListener
                            Looper.getMainLooper()
                        )
                        listener.onGpsStatusChanged("Using AOSP GPS Provider...", "")
                    } else if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            5000L, // minTime (ms)
                            10f,   // minDistance (meters)
                            this, // <--- Use 'this' as LocationListener
                            Looper.getMainLooper()
                        )
                        listener.onGpsStatusChanged("Using AOSP Network Provider...", "")
                    }
                } else {
                    listener.onGpsStatusChanged("GPS and Network providers disabled. Enable location in settings.", "")
                }
            } catch (e: SecurityException) {
                listener.onGpsStatusChanged("Location permission error for AOSP LocationManager.", "")
            }
        }
        // Corrected line:
        listener.onGpsStatusChanged("", context.getString(R.string.gps_accuracy_unknown)) // Initial status for accuracy
    }

    fun stopLocationUpdates() {
        if (useFusedLocationClient) {
            fusedLocationClient.removeLocationUpdates(playServicesLocationCallback)
        } else {
            locationManager.removeUpdates(this) // <--- Use 'this'
        }
    }

    // --- Private Helper Methods ---

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context) // <--- CORRECTED LINE
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun createPlayServicesLocationRequest() {
        playServicesLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f)
            .build()
    }

    private fun createPlayServicesLocationCallback() {
        playServicesLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationAndDeclination(location)
                } ?: run {
                    listener.onGpsStatusChanged(context.getString(R.string.gps_status_acquiring), "")
                }
            }
        }
    }

    // LocationTracker itself implements LocationListener, so this is the callback
    override fun onLocationChanged(location: Location) {
        updateLocationAndDeclination(location)
    }

    override fun onProviderEnabled(provider: String) {
        Toast.makeText(context, "$provider enabled", Toast.LENGTH_SHORT).show()
        listener.onGpsStatusChanged("GPS provider enabled.", "")
    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(context, "$provider disabled", Toast.LENGTH_SHORT).show()
        listener.onGpsStatusChanged("GPS provider disabled. Check settings.", context.getString(R.string.gps_accuracy_unknown))
        currentLocation = null
        magneticDeclination = 0f
        listener.onLocationUpdated(null, 0f) // Inform listener that location is unavailable
    }

    private fun updateLocationAndDeclination(location: Location) {
        currentLocation = location
        val lat = location.latitude
        val long = location.longitude
        val alt = location.altitude
        val accuracy = location.accuracy

        listener.onGpsStatusChanged(context.getString(R.string.gps_status_data, lat, long, alt), context.getString(R.string.gps_accuracy_data, accuracy))

        val geoField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            System.currentTimeMillis()
        )
        magneticDeclination = geoField.declination
        listener.onLocationUpdated(currentLocation, magneticDeclination)
    }
}