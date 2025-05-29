package com.vektor.offgrid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast // <--- ADDED THIS IMPORT
import kotlin.math.abs

// Define a callback interface for MainActivity to receive sensor updates
interface CompassSensorListener {
    fun onCompassUpdate(
        azimuth: Float,
        pitch: Float,
        roll: Float,
        accuracy: Int,
        isSnapping: Boolean
    )
    fun onSensorAccuracyChanged(accuracy: Int)
}

class CompassSensorManager(private val context: Context, private val listener: CompassSensorListener) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magneticFieldReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9) // Use a single matrix for calculation
    private val orientationAngles = FloatArray(3) // Use a single array for angles

    var currentAzimuthForAnimation = 0f // Current azimuth for animation (will be passed to listener)
        internal set // Only allow internal modification

    var currentPitch = 0f
        private set
    var currentRoll = 0f
        private set

    private val snapTolerance = 3f // Degrees for cardinal direction snapping

    init {
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun start() {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            // Fallback to accelerometer and magnetometer if ROTATION_VECTOR is not available
            if (accelerometer != null && magnetometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            } else {
                // Inform the listener if no suitable sensor is found
                listener.onCompassUpdate(0f, 0f, 0f, SensorManager.SENSOR_STATUS_UNRELIABLE, false)
                listener.onSensorAccuracyChanged(SensorManager.SENSOR_STATUS_UNRELIABLE)
                Toast.makeText(context, "Error: No suitable compass sensor found!", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawAzimuth: Float? = null // Raw azimuth from sensor (magnetic)

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            currentRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticFieldReading, 0, magneticFieldReading.size)
        }

        // If using accelerometer and magnetometer fallback
        if (rotationSensor == null && accelerometerReading[0] != 0f && magneticFieldReading[0] != 0f) {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magneticFieldReading)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            rawAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat() // Update pitch/roll here too
            currentRoll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        }

        // Use 'let' to ensure rawAzimuth is non-null for subsequent operations
        rawAzimuth?.let { actualRawAzimuth ->
            // Ensure azimuth is 0-360 and positive
            var processedAzimuth = (actualRawAzimuth + 360) % 360

            var isSnapping = false
            val cardinalAngles = floatArrayOf(0f, 90f, 180f, 270f)
            for (cardinalAngle in cardinalAngles) {
                val diff = abs(processedAzimuth - cardinalAngle) // 'processedAzimuth' is now Float
                val wrappedDiff = minOf(diff, 360 - diff)
                if (wrappedDiff <= snapTolerance) {
                    processedAzimuth = cardinalAngle // Snap to cardinal direction
                    isSnapping = true
                    break
                }
            }
            currentAzimuthForAnimation = processedAzimuth // 'processedAzimuth' is now Float

            listener.onCompassUpdate(processedAzimuth, currentPitch, currentRoll, event.accuracy, isSnapping) // 'processedAzimuth' is now Float
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            listener.onSensorAccuracyChanged(accuracy)
        }
    }
}