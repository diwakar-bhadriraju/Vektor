package com.vektor.offgrid

import android.content.Context
import androidx.core.content.edit

class AppPreferences(context: Context) {

    private val prefsName = "CompassPrefs"
    private val keyCompassOffset = "compassOffset"
    private val keyTargetAzimuth = "targetAzimuth"
    private val keyIsTargetSet = "isTargetSet"

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun getCompassOffset(): Float {
        return prefs.getFloat(keyCompassOffset, 0f)
    }

    fun saveCompassOffset(offset: Float) {
        prefs.edit { putFloat(keyCompassOffset, offset) }
    }

    fun getTargetAzimuth(): Float? {
        // Using a default value that signifies "not set" if target was not stored
        val storedValue = prefs.getFloat(keyTargetAzimuth, -1f)
        return if (storedValue == -1f && !isTargetSet()) null else storedValue
    }

    fun saveTargetAzimuth(azimuth: Float?) {
        prefs.edit().apply {
            if (azimuth != null) {
                putFloat(keyTargetAzimuth, azimuth)
            } else {
                remove(keyTargetAzimuth) // Remove if target is cleared
            }
            apply()
        }
    }

    fun isTargetSet(): Boolean {
        return prefs.getBoolean(keyIsTargetSet, false)
    }

    fun saveIsTargetSet(isSet: Boolean) {
        prefs.edit { putBoolean(keyIsTargetSet, isSet) }
    }
}