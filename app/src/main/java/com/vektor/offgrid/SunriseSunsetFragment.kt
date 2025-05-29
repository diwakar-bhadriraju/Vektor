package com.vektor.offgrid

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SunriseSunsetFragment : Fragment() {

    private lateinit var tvSunriseTime: TextView
    private lateinit var tvSunsetTime: TextView
    private lateinit var tvLocation: TextView
    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @RequiresApi(Build.VERSION_CODES.N)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Toast.makeText(context, "Location permission granted!", Toast.LENGTH_SHORT).show()
            fetchLocationAndSunriseSunset()
        } else {
            Toast.makeText(context, "Location permission denied. Cannot fetch sunrise/sunset.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            tvLocation.text = "Location access denied."
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sunrise_sunset, container, false)

        tvSunriseTime = view.findViewById(R.id.tvSunriseTime)
        tvSunsetTime = view.findViewById(R.id.tvSunsetTime)
        tvLocation = view.findViewById(R.id.tvLocation)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        progressBar = view.findViewById(R.id.progressBar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        btnRefresh.setOnClickListener {
            checkAndRequestLocationPermissions()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestLocationPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (isLocationEnabled()) {
                fetchLocationAndSunriseSunset()
            } else {
                Toast.makeText(context, "Please enable location services in device settings.", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                tvLocation.text = "Location services disabled."
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
            }
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    private fun fetchLocationAndSunriseSunset() {
        if (!isInternetAvailable()) {
            Toast.makeText(context, "No internet connection. Cannot fetch sunrise/sunset data.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            tvLocation.text = "No internet."
            tvSunriseTime.text = "--:--"
            tvSunsetTime.text = "--:--"
            return
        }

        progressBar.visibility = View.VISIBLE
        tvLocation.text = "Getting location..."
        tvSunriseTime.text = ""
        tvSunsetTime.text = ""

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    tvLocation.text = "Lat: %.4f, Lon: %.4f".format(latitude, longitude)
                    fetchSunriseSunsetTimes(latitude, longitude)
                } else {
                    Toast.makeText(context, "Unable to get current location. Please try again.", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    tvLocation.text = "Location not found."
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("SunriseSunsetFragment", "Location error: ${e.message}", e)
                progressBar.visibility = View.GONE
                tvLocation.text = "Error getting location."
            }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun fetchSunriseSunsetTimes(latitude: Double, longitude: Double) {
        // We removed the `withContext(Dispatchers.Main)` block here
        // as this function will be called from a UI context or within a coroutine.
        // The check for internet is handled in fetchLocationAndSunriseSunset.
        if (!isInternetAvailable()) {
            Toast.makeText(context, "No internet connection. Cannot fetch sunrise/sunset data.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            tvSunriseTime.text = "--:--"
            tvSunsetTime.text = "--:--"
            return
        }


        progressBar.visibility = View.VISIBLE
        tvSunriseTime.text = "Fetching data..."
        tvSunsetTime.text = "Fetching data..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = "https://api.sunrise-sunset.org/json?lat=$latitude&lng=$longitude&date=today&formatted=0"
                val response = URL(apiUrl).readText()
                val jsonResponse = JSONObject(response)
                val status = jsonResponse.getString("status")

                if (status == "OK") {
                    val results = jsonResponse.getJSONObject("results")
                    val sunriseUtc = results.getString("sunrise")
                    val sunsetUtc = results.getString("sunset")

                    val inputDateFormat =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault())
                        } else {
                            TODO("VERSION.SDK_INT < N")
                        }
                    inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")

                    val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

                    val sunriseDate: Date? = inputDateFormat.parse(sunriseUtc)
                    val sunsetDate: Date? = inputDateFormat.parse(sunsetUtc)

                    withContext(Dispatchers.Main) {
                        if (sunriseDate != null && sunsetDate != null) {
                            tvSunriseTime.text = buildString {
        append("Sunrise: ")
        append(outputFormat.format(sunriseDate))
    }
                            tvSunsetTime.text = buildString {
        append("Sunset: ")
        append(outputFormat.format(sunsetDate))
    }
                        } else {
                            tvSunriseTime.text = "Error parsing time"
                            tvSunsetTime.text = "Error parsing time"
                            Toast.makeText(context, "Failed to parse sunrise/sunset times.", Toast.LENGTH_LONG).show()
                        }
                        progressBar.visibility = View.GONE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error fetching sunrise/sunset data: $status", Toast.LENGTH_LONG).show()
                        tvSunriseTime.text = "Error"
                        tvSunsetTime.text = "Error"
                        progressBar.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network or parsing error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("SunriseSunsetFragment", "Network/Parsing error: ${e.message}", e)
                    tvSunriseTime.text = "Network Error"
                    tvSunsetTime.text = "Network Error"
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
}