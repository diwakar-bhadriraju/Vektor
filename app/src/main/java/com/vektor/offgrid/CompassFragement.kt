package com.vektor.offgrid

import android.Manifest
import android.animation.ObjectAnimator
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.math.abs
import kotlin.math.roundToInt

class CompassFragment : Fragment(), CompassSensorListener, LocationUpdateListener {

    // --- UI Elements (moved from MainActivity) ---
    private lateinit var compassDial: ImageView
    private lateinit var needleImageView: ImageView
    private lateinit var targetNeedleImageView: ImageView
    private lateinit var directionTextView: TextView
    private lateinit var accuracyTextView: TextView
    private lateinit var calibrationTextView: TextView
    private lateinit var gpsStatusTextView: TextView
    private lateinit var gpsAccuracyTextView: TextView
    private lateinit var setTargetButton: Button
    private lateinit var clearTargetButton: Button
    private lateinit var freezeButton: Button
    private lateinit var targetGuidanceTextView: TextView
    private lateinit var setOffsetButton: Button
    private lateinit var calibrateNorthButton: Button

    // --- Managers (moved from MainActivity) ---
    private lateinit var appPreferences: AppPreferences
    private lateinit var compassSensorManager: CompassSensorManager
    private lateinit var locationTracker: LocationTracker
    private lateinit var hapticFeedbackManager: HapticFeedbackManager

    // --- State Variables (moved from MainActivity) ---
    private var compassOffset: Float = 0f
    private var targetAzimuth: Float? = null
    private var isTargetSet = false
    private var isFrozen = false
    private var magneticDeclination: Float = 0f
    private var currentLocation: Location? = null

    // --- Permission Launcher (moved from MainActivity) ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            locationTracker.startLocationUpdates()
        } else {
            gpsStatusTextView.text = getString(R.string.gps_status_unavailable)
            Toast.makeText(requireContext(), "Location permission denied. Cannot get GPS data.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_compass, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- UI Initialization (NOW using 'view.findViewById' because it's a Fragment) ---
        compassDial = view.findViewById(R.id.compassDial)
        needleImageView = view.findViewById(R.id.needleImageView)
        targetNeedleImageView = view.findViewById(R.id.targetNeedleImageView)
        directionTextView = view.findViewById(R.id.directionTextView)
        accuracyTextView = view.findViewById(R.id.accuracyTextView)
        calibrationTextView = view.findViewById(R.id.calibrationTextView)
        gpsStatusTextView = view.findViewById(R.id.gpsStatusTextView)
        gpsAccuracyTextView = view.findViewById(R.id.gpsAccuracyTextView)
        setTargetButton = view.findViewById(R.id.setTargetButton)
        clearTargetButton = view.findViewById(R.id.clearTargetButton)
        freezeButton = view.findViewById(R.id.freezeButton)
        targetGuidanceTextView = view.findViewById(R.id.targetGuidanceTextView)
        setOffsetButton = view.findViewById(R.id.setOffsetButton)
        calibrateNorthButton = view.findViewById(R.id.calibrateNorthButton)

        // --- Initialize Managers (using 'requireContext()' as Fragment doesn't have 'this' as Context) ---
        appPreferences = AppPreferences(requireContext())
        compassSensorManager = CompassSensorManager(requireContext(), this)
        hapticFeedbackManager = HapticFeedbackManager(requireContext())
        locationTracker = LocationTracker(requireContext(), this)

        // --- Load state from preferences ---
        compassOffset = appPreferences.getCompassOffset()
        targetAzimuth = appPreferences.getTargetAzimuth()
        isTargetSet = appPreferences.isTargetSet()

        // Initial UI state for target
        if (isTargetSet && targetAzimuth != null) {
            targetNeedleImageView.visibility = View.VISIBLE
            setTargetButton.visibility = View.GONE
            clearTargetButton.visibility = View.VISIBLE
            // Update guidance if current location/azimuth is known
            val currentTrueNorth = (-(compassSensorManager.currentAzimuthForAnimation) + 360) % 360
            updateTargetGuidance(currentTrueNorth, targetAzimuth!!)
        } else {
            targetNeedleImageView.visibility = View.GONE
            setTargetButton.visibility = View.VISIBLE
            clearTargetButton.visibility = View.GONE
        }

        // --- Button Listeners (moved from MainActivity) ---
        setTargetButton.setOnClickListener {
            showSetTargetDialog()
        }

        clearTargetButton.setOnClickListener {
            targetAzimuth = null
            isTargetSet = false
            appPreferences.saveTargetAzimuth(null)
            appPreferences.saveIsTargetSet(false)
            targetNeedleImageView.visibility = View.GONE
            targetGuidanceTextView.text = getString(R.string.target_guidance_none)
            clearTargetButton.visibility = View.GONE
            setTargetButton.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Target cleared.", Toast.LENGTH_SHORT).show()
        }

        freezeButton.setOnClickListener {
            isFrozen = !isFrozen
            if (isFrozen) {
                compassSensorManager.stop()
                locationTracker.stopLocationUpdates()
                freezeButton.text = getString(R.string.unfreeze_compass)
                Toast.makeText(requireContext(), "Compass frozen.", Toast.LENGTH_SHORT).show()
            } else {
                compassSensorManager.start()
                locationTracker.checkLocationPermissionsAndStartUpdates()
                freezeButton.text = getString(R.string.freeze_compass)
                Toast.makeText(requireContext(), "Compass unfrozen.", Toast.LENGTH_SHORT).show()
            }
        }

        setOffsetButton.setOnClickListener { showSetCompassOffsetDialog() }
        calibrateNorthButton.setOnClickListener { showGuidedCalibrationDialog() }
    }

    // --- Lifecycle Callbacks (moved from MainActivity, now Fragment lifecycle) ---
    override fun onResume() {
        super.onResume()
        if (!isFrozen) {
            compassSensorManager.start()
            locationTracker.checkLocationPermissionsAndStartUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        compassSensorManager.stop()
        locationTracker.stopLocationUpdates()
        hapticFeedbackManager.cancelVibrations()
    }

    // --- CompassSensorListener Implementation (moved from MainActivity) ---
    override fun onCompassUpdate(
        azimuth: Float,
        pitch: Float,
        roll: Float,
        accuracy: Int,
        isSnapping: Boolean
    ) {
        // Note: The rotation value for ImageView is clockwise positive.
        // To make the needle point to True North, we rotate it by -azimuth degrees.
        val mainNeedleAnimator = ObjectAnimator.ofFloat(needleImageView, "rotation", compassSensorManager.currentAzimuthForAnimation, -(azimuth))
        mainNeedleAnimator.duration = 300
        mainNeedleAnimator.start()
        compassSensorManager.currentAzimuthForAnimation = -(azimuth) // Update for next animation

        directionTextView.text = getDirectionFromAzimuth(azimuth)
        updateCalibrationStatus(pitch, roll)

        // Animate target needle if set
        if (isTargetSet && targetAzimuth != null) {
            targetNeedleImageView.rotation = targetAzimuth!! // Directly set rotation, no animation
            targetNeedleImageView.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                updateTargetGuidance(azimuth, targetAzimuth!!)
            }
        } else {
            targetNeedleImageView.visibility = View.GONE
            targetGuidanceTextView.text = getString(R.string.target_guidance_none)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hapticFeedbackManager.handleHapticFeedback(azimuth, isSnapping)
        }
    }

    override fun onSensorAccuracyChanged(accuracy: Int) {
        val accuracyString = when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> getString(R.string.accuracy_status_unreliable)
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> getString(R.string.accuracy_status_low)
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> getString(R.string.accuracy_status_medium)
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> getString(R.string.accuracy_status_high)
            else -> getString(R.string.accuracy_status_unknown)
        }
        accuracyTextView.text = accuracyString
    }

    // --- LocationUpdateListener Implementation (moved from MainActivity) ---
    override fun onLocationUpdated(location: Location?, declination: Float) {
        currentLocation = location
        magneticDeclination = declination
    }

    override fun onGpsStatusChanged(status: String, accuracy: String) {
        gpsStatusTextView.text = status
        gpsAccuracyTextView.text = accuracy
    }

    override fun onRequestLocationPermissions() {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    // --- Compass Utility Functions (moved from MainActivity) ---
    private fun getDirectionFromAzimuth(azimuth: Float): String {
        // Apply magnetic declination and user offset here before converting to direction string
        val adjustedAzimuth = (azimuth + magneticDeclination + compassOffset + 360) % 360
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((adjustedAzimuth + 22.5f) % 360 / 45).toInt()
        val cardinalDirection = directions[index]
        val roundedAzimuth = adjustedAzimuth.roundToInt()
        return "$cardinalDirection ($roundedAzimuth°)"
    }

    private fun updateCalibrationStatus(pitch: Float, roll: Float) {
        val tiltTolerance = 15f
        val isLevel = abs(pitch) <= tiltTolerance && abs(roll) <= tiltTolerance
        val currentAccuracyText = accuracyTextView.text.toString()

        if (isLevel) {
            calibrationTextView.text = getString(R.string.calibration_status_ok)
            calibrationTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
        } else {
            calibrationTextView.text = getString(R.string.calibration_status_tilt)
            calibrationTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
        }

        if (currentAccuracyText == getString(R.string.accuracy_status_unreliable)) {
            calibrationTextView.text = getString(R.string.compass_unreliable_tilt)
            calibrationTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
        }
    }

    // --- Target Guidance Functions (moved from MainActivity) ---
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTargetGuidance(currentAngle: Float, targetAngle: Float) {
        val diff = (targetAngle - currentAngle + 360) % 360
        val degreesToTurn: Float

        val targetAlignmentTolerance = 3f

        if (diff <= targetAlignmentTolerance || diff >= (360 - targetAlignmentTolerance)) {
            targetGuidanceTextView.text = getString(R.string.target_guidance_aligned)
            targetGuidanceTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            hapticFeedbackManager.vibrateTargetAlignment()
        } else if (diff > 180) {
            degreesToTurn = 360 - diff
            targetGuidanceTextView.text = getString(R.string.target_guidance_turn_right, degreesToTurn.roundToInt().toFloat())
            targetGuidanceTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
        } else {
            degreesToTurn = diff
            targetGuidanceTextView.text = getString(R.string.target_guidance_turn_left, degreesToTurn.roundToInt().toFloat())
            targetGuidanceTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light))
        }
    }

    // Show dialog to set target bearing (moved from MainActivity)
    private fun showSetTargetDialog() {
        val builder = AlertDialog.Builder(requireContext()) // Use requireContext()
        builder.setTitle(getString(R.string.set_target))

        val input = EditText(requireContext()) // Use requireContext()
        input.hint = getString(R.string.enter_target_degrees)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.set)) { dialog, _ ->
            val enteredText = input.text.toString()
            if (enteredText.isNotEmpty()) {
                try {
                    val bearing = enteredText.toFloat()
                    if (bearing >= 0 && bearing < 360) {
                        targetAzimuth = bearing
                        isTargetSet = true
                        appPreferences.saveTargetAzimuth(bearing)
                        appPreferences.saveIsTargetSet(true)
                        Toast.makeText(requireContext(), getString(R.string.target_set, bearing.roundToInt().toFloat()), Toast.LENGTH_SHORT).show()
                        setTargetButton.visibility = View.GONE
                        clearTargetButton.visibility = View.VISIBLE
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val currentTrueNorth = (-(compassSensorManager.currentAzimuthForAnimation) + 360) % 360
                            updateTargetGuidance(currentTrueNorth, targetAzimuth!!)
                        }
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.invalid_target_degrees), Toast.LENGTH_LONG).show()
                    }
                } catch (_: NumberFormatException) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_target_degrees), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.enter_target_degrees), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // --- Show Compass Offset Dialog (Manual Input) (moved from MainActivity) ---
    private fun showSetCompassOffsetDialog() {
        val builder = AlertDialog.Builder(requireContext()) // Use requireContext()
        builder.setTitle(getString(R.string.set_compass_offset))

        val input = EditText(requireContext()) // Use requireContext()
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        input.setText(compassOffset.toString())
        input.setSelectAllOnFocus(true)
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.set)) { dialog, _ ->
            val offsetStr = input.text.toString()
            if (offsetStr.isNotBlank()) {
                try {
                    val newOffset = offsetStr.toFloat()
                    compassOffset = newOffset
                    appPreferences.saveCompassOffset(newOffset)
                    Toast.makeText(requireContext(), getString(R.string.compass_offset_saved, newOffset), Toast.LENGTH_SHORT).show()
                } catch (_: NumberFormatException) {
                    Toast.makeText(requireContext(), getString(R.string.invalid_offset_value), Toast.LENGTH_SHORT).show()
                }
            } else {
                compassOffset = 0f
                appPreferences.saveCompassOffset(0f)
                Toast.makeText(requireContext(), getString(R.string.compass_offset_reset), Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // --- Show Guided Calibration Dialog (moved from MainActivity) ---
    private fun showGuidedCalibrationDialog() {
        val builder = AlertDialog.Builder(requireContext()) // Use requireContext()
        builder.setTitle(getString(R.string.guided_calibration_title))
        builder.setMessage(getString(R.string.guided_calibration_message))

        builder.setPositiveButton(getString(R.string.calibrate_now)) { dialog, _ ->
            val currentSensorTrueAzimuth = (-(compassSensorManager.currentAzimuthForAnimation) + 360) % 360
            val calculatedOffset = (0f - currentSensorTrueAzimuth + 360f) % 360f
            val finalOffset = if (calculatedOffset > 180f) calculatedOffset - 360f else calculatedOffset

            compassOffset = finalOffset
            appPreferences.saveCompassOffset(finalOffset)
            Toast.makeText(requireContext(), getString(R.string.guided_calibration_success, finalOffset), Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}