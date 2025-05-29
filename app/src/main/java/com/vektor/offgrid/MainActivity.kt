package com.vektor.offgrid

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // --- Navigation Drawer Elements ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Toolbar and Navigation Drawer Setup ---
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        // --- Load the default fragment (IntroductionFragment) and set default selected menu item ---
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, IntroductionFragment())
                .commit()
            navView.setCheckedItem(R.id.nav_home)
            toolbar.title = getString(R.string.menu_home)
        } else {
            // Restore toolbar title on configuration change based on current fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is IntroductionFragment) {
                toolbar.title = getString(R.string.menu_home)
            } else if (currentFragment is CompassFragment) {
                toolbar.title = getString(R.string.menu_compass_altitude)
            } else if (currentFragment is SosFragment) {
                toolbar.title = "Morse Code Signal Generator"
            } else if (currentFragment is SunriseSunsetFragment) {
                toolbar.title = "Sunrise & Sunset"
            } else if (currentFragment is WeatherFragment) { // <--- Added for WeatherFragment
                toolbar.title = "Weather Snapshot"
            } else if (currentFragment is MagneticFieldFragment) { // <--- Added for MagneticFieldFragment
                toolbar.title = "Magnetic Field Strength"
            }
            // Add more else if for other fragments as you create them
        }

        // --- New: Handle back press using OnBackPressedDispatcher ---
        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()

                    // After popping, update the toolbar title to match the now visible fragment.
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment is IntroductionFragment) {
                        toolbar.title = getString(R.string.menu_home)
                    } else if (currentFragment is CompassFragment) {
                        toolbar.title = getString(R.string.menu_compass_altitude)
                    } else if (currentFragment is SosFragment) {
                        toolbar.title = "Morse Code Signal Generator"
                    } else if (currentFragment is SunriseSunsetFragment) {
                        toolbar.title = "Sunrise & Sunset"
                    } else if (currentFragment is WeatherFragment) { // <--- Added for WeatherFragment
                        toolbar.title = "Weather Snapshot"
                    } else if (currentFragment is MagneticFieldFragment) { // <--- Added for MagneticFieldFragment
                        toolbar.title = "Magnetic Field Strength"
                    } else {
                        // Fallback title if the fragment type is not explicitly handled.
                        toolbar.title = "OffGrid App" // Default app name
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // --- Navigation Drawer Item Click Listener (Handles Fragment Replacement) ---
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)

        drawerLayout.postDelayed({
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, IntroductionFragment())
                        .commit()
                    toolbar.title = getString(R.string.menu_home)
                }
                R.id.nav_compass_altitude -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CompassFragment())
                        .commit()
                    toolbar.title = getString(R.string.menu_compass_altitude)
                }
                R.id.nav_gps_tracking -> {
                    Toast.makeText(this, "GPS Path Tracking selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "GPS Path Tracking"
                }
                R.id.nav_offline_map -> {
                    Toast.makeText(this, "Offline Map & Waypoints selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Offline Map"
                }
                R.id.nav_sunrise_sunset -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SunriseSunsetFragment())
                        .commit()
                    toolbar.title = "Sunrise & Sunset"
                }
                R.id.nav_silent_sos -> {
                    Toast.makeText(this, "Silent SOS Trigger selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Silent SOS"
                }
                R.id.nav_intrusion_detection -> {
                    Toast.makeText(this, "Intrusion Detection selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Intrusion Detection"
                }
                R.id.nav_sos_signals -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SosFragment())
                        .commit()
                    toolbar.title = "Morse Code Signal Generator"
                }
                R.id.nav_stealth_mode -> {
                    Toast.makeText(this, "Stealth Mode selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Stealth Mode"
                }
                R.id.nav_battery_saver -> {
                    Toast.makeText(this, "Battery Saver selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Battery Saver"
                }
                R.id.nav_sensor_check -> {
                    Toast.makeText(this, "Sensor Check selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Sensor Check"
                }
                R.id.nav_step_counter -> {
                    Toast.makeText(this, "Step Counter selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Step Counter"
                }
                R.id.nav_weather_snapshot -> {
                    // <--- Replaced Toast with Fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, WeatherFragment())
                        .commit()
                    toolbar.title = "Weather Snapshot"
                }
                R.id.nav_magnetic_strength -> {
                    // <--- Replaced Toast with Fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MagneticFieldFragment())
                        .commit()
                    toolbar.title = "Magnetic Field Strength"
                }
                R.id.nav_survival_guide -> {
                    Toast.makeText(this, "Offline Survival Guide selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Survival Guide"
                }
                R.id.nav_bluetooth_scanner -> {
                    Toast.makeText(this, "BT Device Scanner selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "Bluetooth Scanner"
                }
                R.id.nav_p2p_messaging -> {
                    Toast.makeText(this, "P2P Messaging selected", Toast.LENGTH_SHORT).show()
                    toolbar.title = "P2P Messaging"
                }
            }
        }, 300)

        return true
    }
}