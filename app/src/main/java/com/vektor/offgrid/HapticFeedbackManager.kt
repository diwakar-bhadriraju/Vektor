package com.vektor.offgrid

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi

class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var lastStrongVibrationTime = 0L
    private var lastLightVibrationTime = 0L
    private var lastTargetVibrationTime = 0L
    private val strongVibrationCooldown = 3000L // 3 seconds
    private val lightVibrationCooldown = 100L    // 0.1 seconds
    private val targetVibrationCooldown = 500L   // 0.5 seconds
    private val targetAlignmentTolerance = 3f    // +/- 3 degrees for target alignment vibe
    private var isVibratingStrongly = false // Flag to track strong vibration state

    private val vibrationTolerance = 5f // For cardinal snaps

    fun handleHapticFeedback(azimuth: Float, isSnapped: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !vibrator.hasVibrator()) {
            isVibratingStrongly = false
            return
        }

        val now = System.currentTimeMillis()

        val isNorth = azimuth < vibrationTolerance || azimuth > (360 - vibrationTolerance)
        val isEast = azimuth in (90f - vibrationTolerance)..(90f + vibrationTolerance)
        val isSouth = azimuth in (180f - vibrationTolerance)..(180f + vibrationTolerance)
        val isWest = azimuth in (270f - vibrationTolerance)..(270f + vibrationTolerance)

        val isNearCardinal = isNorth || isEast || isSouth || isWest

        if (isSnapped) {
            if (isVibratingStrongly) {
                vibrator.cancel()
                isVibratingStrongly = false
            }
            if (isNearCardinal && now - lastStrongVibrationTime > strongVibrationCooldown) {
                lastStrongVibrationTime = now
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                isVibratingStrongly = true
            }
        } else {
            if (isVibratingStrongly) {
                vibrator.cancel()
                isVibratingStrongly = false
            }
            if (now - lastLightVibrationTime > lightVibrationCooldown) {
                lastLightVibrationTime = now
                vibrator.vibrate(VibrationEffect.createOneShot(2, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    fun vibrateTargetAlignment() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !vibrator.hasVibrator()) return

        val now = System.currentTimeMillis()
        if (now - lastTargetVibrationTime > targetVibrationCooldown) {
            lastTargetVibrationTime = now
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)) // Short distinct vibe
        }
    }

    fun cancelVibrations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
            vibrator.cancel()
            isVibratingStrongly = false
        }
    }
}