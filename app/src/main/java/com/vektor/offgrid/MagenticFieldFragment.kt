package com.vektor.offgrid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.math.sqrt

class MagneticFieldFragment : Fragment(), SensorEventListener {

    private lateinit var tvMagneticX: TextView
    private lateinit var tvMagneticY: TextView
    private lateinit var tvMagneticZ: TextView
    private lateinit var tvMagneticTotal: TextView
    private lateinit var tvSensorAvailability: TextView

    private var sensorManager: SensorManager? = null
    private var magneticFieldSensor: Sensor? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_magnetic_field, container, false)

        tvMagneticX = view.findViewById(R.id.tvMagneticX)
        tvMagneticY = view.findViewById(R.id.tvMagneticY)
        tvMagneticZ = view.findViewById(R.id.tvMagneticZ)
        tvMagneticTotal = view.findViewById(R.id.tvMagneticTotal)
        tvSensorAvailability = view.findViewById(R.id.tvSensorAvailability)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticFieldSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticFieldSensor == null) {
            tvSensorAvailability.text = buildString {
        append("Magnetic Field Sensor: NOT AVAILABLE")
    }
            tvSensorAvailability.visibility = View.VISIBLE
            Toast.makeText(context, "Magnetic Field Sensor not available on this device.", Toast.LENGTH_LONG).show()
        } else {
            tvSensorAvailability.text = buildString {
        append("Magnetic Field Sensor: AVAILABLE")
    }
            tvSensorAvailability.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the listener when the fragment is active
        magneticFieldSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener when the fragment is paused to save battery
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val magneticX = event.values[0]
            val magneticY = event.values[1]
            val magneticZ = event.values[2]

            // Calculate total magnetic field strength (magnitude)
            val totalMagnetic = sqrt(magneticX * magneticX + magneticY * magneticY + magneticZ * magneticZ)

            tvMagneticX.text = buildString {
        append("X: %.2f µT")
    }.format(magneticX)
            tvMagneticY.text = buildString {
        append("Y: %.2f µT")
    }.format(magneticY)
            tvMagneticZ.text = buildString {
        append("Z: %.2f µT")
    }.format(magneticZ)
            tvMagneticTotal.text = buildString {
        append("Total: %.2f µT")
    }.format(totalMagnetic)

            // Optional: Provide some visual feedback for strong fields (e.g., nearby magnets)
            if (totalMagnetic > 100) { // Typical range is 20-70 µT, so 100 is quite high
                tvMagneticTotal.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            } else {
                tvMagneticTotal.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black)) // Or default text color
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not typically used for magnetic field sensor, but can be implemented if needed
        // For example, to display sensor accuracy status.
    }
}