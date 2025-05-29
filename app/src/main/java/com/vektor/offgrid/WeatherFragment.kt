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
import java.util.Locale

class WeatherFragment : Fragment() {

    private val OPEN_WEATHER_MAP_API_KEY = "YOUR_OPENWEATHERMAP_API_KEY" // <--- REPLACE THIS

    private lateinit var tvWeatherLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvWindSpeed: TextView
    private lateinit var btnWeatherRefresh: Button
    private lateinit var weatherProgressBar: ProgressBar

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Toast.makeText(context, "Location permission granted!", Toast.LENGTH_SHORT).show()
            fetchLocationAndWeather()
        } else {
            Toast.makeText(context, "Location permission denied. Cannot fetch weather.", Toast.LENGTH_LONG).show()
            weatherProgressBar.visibility = View.GONE
            tvWeatherLocation.text = "Location access denied."
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather, container, false)

        tvWeatherLocation = view.findViewById(R.id.tvWeatherLocation)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvWindSpeed = view.findViewById(R.id.tvWindSpeed)
        btnWeatherRefresh = view.findViewById(R.id.btnWeatherRefresh)
        weatherProgressBar = view.findViewById(R.id.weatherProgressBar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        btnWeatherRefresh.setOnClickListener {
            checkAndRequestLocationPermissions()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestLocationPermissions()
    }

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
                fetchLocationAndWeather()
            } else {
                Toast.makeText(context, "Please enable location services in device settings.", Toast.LENGTH_LONG).show()
                weatherProgressBar.visibility = View.GONE
                tvWeatherLocation.text = "Location services disabled."
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

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndWeather() {
        if (!isInternetAvailable()) {
            Toast.makeText(context, "No internet connection. Cannot fetch weather data.", Toast.LENGTH_LONG).show()
            weatherProgressBar.visibility = View.GONE
            tvWeatherLocation.text = "No internet."
            tvTemperature.text = "--.-°C"
            tvDescription.text = "--"
            tvHumidity.text = "--%"
            tvWindSpeed.text = "--.- m/s"
            return
        }

        weatherProgressBar.visibility = View.VISIBLE
        tvWeatherLocation.text = "Getting location..."
        tvTemperature.text = ""
        tvDescription.text = ""
        tvHumidity.text = ""
        tvWindSpeed.text = ""

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    tvWeatherLocation.text = "Lat: %.4f, Lon: %.4f".format(latitude, longitude)
                    fetchWeatherData(latitude, longitude)
                } else {
                    Toast.makeText(context, "Unable to get current location. Please try again.", Toast.LENGTH_LONG).show()
                    weatherProgressBar.visibility = View.GONE
                    tvWeatherLocation.text = "Location not found."
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("WeatherFragment", "Location error: ${e.message}", e)
                weatherProgressBar.visibility = View.GONE
                tvWeatherLocation.text = "Error getting location."
            }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        if (OPEN_WEATHER_MAP_API_KEY == "YOUR_OPENWEATHERMAP_API_KEY" || OPEN_WEATHER_MAP_API_KEY.isEmpty()) {
            Toast.makeText(context, "Please set your OpenWeatherMap API Key in WeatherFragment.kt", Toast.LENGTH_LONG).show()
            weatherProgressBar.visibility = View.GONE
            return
        }
        // We removed the `withContext(Dispatchers.Main)` block here
        // as this function will be called from a UI context or within a coroutine.
        // The check for internet is handled in fetchLocationAndWeather.
        if (!isInternetAvailable()) {
            Toast.makeText(context, "No internet connection. Cannot fetch weather data.", Toast.LENGTH_LONG).show()
            weatherProgressBar.visibility = View.GONE
            tvTemperature.text = "--.-°C"
            tvDescription.text = "--"
            tvHumidity.text = "--%"
            tvWindSpeed.text = "--.- m/s"
            return
        }

        weatherProgressBar.visibility = View.VISIBLE
        tvTemperature.text = "Fetching data..."
        tvDescription.text = "Fetching data..."
        tvHumidity.text = "Fetching data..."
        tvWindSpeed.text = "Fetching data..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$OPEN_WEATHER_MAP_API_KEY"
                val response = URL(apiUrl).readText()
                val jsonResponse = JSONObject(response)

                val main = jsonResponse.getJSONObject("main")
                val temperature = main.getDouble("temp")
                val humidity = main.getInt("humidity")

                val weatherArray = jsonResponse.getJSONArray("weather")
                val weatherObject = weatherArray.getJSONObject(0)
                val description = weatherObject.getString("description")

                val wind = jsonResponse.getJSONObject("wind")
                val windSpeed = wind.getDouble("speed")

                val cityName = jsonResponse.getString("name")

                withContext(Dispatchers.Main) {
                    tvWeatherLocation.text = "Location: $cityName (Lat: %.4f, Lon: %.4f)".format(latitude, longitude)
                    tvTemperature.text = "Temperature: %.1f°C".format(temperature)
                    tvDescription.text = "Condition: ${description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}"
                    tvHumidity.text = "Humidity: $humidity%"
                    tvWindSpeed.text = "Wind: %.1f m/s".format(windSpeed)
                    weatherProgressBar.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Weather data error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("WeatherFragment", "Weather network/parsing error: ${e.message}", e)
                    tvTemperature.text = "Error"
                    tvDescription.text = "Error"
                    tvHumidity.text = "Error"
                    tvWindSpeed.text = "Error"
                    weatherProgressBar.visibility = View.GONE
                }
            }
        }
    }
}