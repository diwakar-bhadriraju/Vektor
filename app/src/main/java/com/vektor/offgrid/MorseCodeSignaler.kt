package com.vektor.offgrid

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MorseCodeSignaler(private val context: Context) {

    private val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var toneGenerator: ToneGenerator
    private val handler = Handler(Looper.getMainLooper()) // Kept for Toast messages on main thread

    // Define custom gap durations as requested by the user
    private val CUSTOM_WORD_GAP_MS = 1500L // 1.5 seconds for word gap
    private val CUSTOM_LOOP_GAP_MS = 2000L // 2 seconds between full loops

    // Morse code timings (adjust as needed for WPM)
    private val dotDuration = 100L    // milliseconds for a dot (e.g., 100ms for 12 WPM)
    private val dashDuration = dotDuration * 3L   // milliseconds (3 times a dot)
    private val elementGap = dotDuration     // Gap between dots/dashes in a letter (1 dot duration)
    private val letterGap = dotDuration * 3L // Gap between letters (3 dot durations)

    private var currentSignalSequence: List<Char> = emptyList()
    private var currentPatternIndex = 0
    @Volatile private var isSignaling = false // Use @Volatile for thread safety

    private val morseCodeMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-.",
        '-' to "-....-", '(' to "-.--.", ')' to "-.--.-", '@' to ".--.-.",
        ' ' to " " // Space for word gap (handled by 'W' in sequence)
    )

    // Using a ScheduledThreadPoolExecutor for more robust timing
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var signalFuture: ScheduledFuture<*>? = null


    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull() // Get the first camera ID, or null
        } catch (e: CameraAccessException) {
            Log.e("MorseCodeSignaler", "Cannot access camera flash: ${e.message}")
            cameraId = null // Ensure it's null if access fails
        }

        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    }

    /**
     * Translates text into a sequence of Morse code elements.
     * 'D' for dot, 'A' for dash, 'E' for element gap, 'L' for letter gap, 'W' for word gap, 'Z' for loop gap.
     */
    private fun textToMorseSequence(text: String): List<Char> {
        val sequence = mutableListOf<Char>()
        var firstCharInWord = true

        text.uppercase().forEach { char ->
            if (char == ' ') {
                if (!firstCharInWord) {
                    sequence.add('W') // Use 'W' for custom word gap
                }
                firstCharInWord = true
                return@forEach
            }

            if (!firstCharInWord) {
                sequence.add('L')
            }
            firstCharInWord = false

            val morsePattern = morseCodeMap[char]
            if (morsePattern != null) {
                var firstElementInChar = true
                morsePattern.forEach { morseElement ->
                    if (!firstElementInChar) {
                        sequence.add('E')
                    }
                    firstElementInChar = false
                    when (morseElement) {
                        '.' -> sequence.add('D')
                        '-' -> sequence.add('A')
                    }
                }
            } else {
                Log.w("MorseCodeSignaler", "Unsupported character for Morse code: $char")
            }
        }
        // After the entire message, add a special marker for the loop gap
        sequence.add('Z') // 'Z' will represent the loop gap
        return sequence
    }

    fun startSignal(text: String, useLight: Boolean, useSound: Boolean) {
        stopSignal() // Stop any ongoing signal first
        if (!useLight && !useSound) {
            handler.post { Toast.makeText(context, "Please select at least one output (light or sound).", Toast.LENGTH_SHORT).show() }
            return
        }

        if (useLight && cameraId == null) {
            handler.post { Toast.makeText(context, "Flashlight not available on this device.", Toast.LENGTH_SHORT).show() }
            if (!useSound) return
        }

        isSignaling = true
        currentSignalSequence = textToMorseSequence(text)
        currentPatternIndex = 0

        if (currentSignalSequence.isNotEmpty()) {
            handler.post { Toast.makeText(context, "Morse signal started...", Toast.LENGTH_SHORT).show() }
            // Start the sequence using the scheduler
            // 'this' refers to the Runnable itself within its own scope.
            signalFuture = scheduler.schedule(signalElementsRunnable(useLight, useSound), 0, TimeUnit.MILLISECONDS)
        } else {
            handler.post { Toast.makeText(context, "No valid Morse code to signal.", Toast.LENGTH_SHORT).show() }
            isSignaling = false
        }
    }

    fun stopSignal() {
        isSignaling = false
        signalFuture?.cancel(true) // Interrupt the scheduled task
        turnFlashlightOff()
        toneGenerator.stopTone()
        currentSignalSequence = emptyList() // Clear sequence
        currentPatternIndex = 0
    }

    private fun turnFlashlightOn() {
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId!!, true)
            } catch (e: CameraAccessException) {
                Log.e("MorseCodeSignaler", "Failed to turn flashlight ON: ${e.message}")
            }
        }
    }

    private fun turnFlashlightOff() {
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId!!, false)
            } catch (e: CameraAccessException) {
                Log.e("MorseCodeSignaler", "Failed to turn flashlight OFF: ${e.message}")
            }
        }
    }

    // THIS IS THE KEY CHANGE: Define as an anonymous object instead of a lambda
    private fun signalElementsRunnable(useLight: Boolean, useSound: Boolean) = object : Runnable {
        override fun run() {
            if (!isSignaling || currentSignalSequence.isEmpty()) {
                stopSignal()
                return
            }

            val element = currentSignalSequence[currentPatternIndex]
            var signalOnDuration = 0L // Duration for which light/sound is ON
            var delayBeforeNextElement = 0L // Delay before the *next* element processing starts

            // Ensure light/sound are off before processing a new element
            if (useLight) turnFlashlightOff()
            if (useSound) toneGenerator.stopTone()

            when (element) {
                'D' -> { // Dot
                    if (useLight) turnFlashlightOn()
                    if (useSound) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, dotDuration.toInt())
                    signalOnDuration = dotDuration
                    delayBeforeNextElement = elementGap
                }
                'A' -> { // Dash
                    if (useLight) turnFlashlightOn()
                    if (useSound) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, dashDuration.toInt())
                    signalOnDuration = dashDuration
                    delayBeforeNextElement = elementGap
                }
                'E' -> { // Element Gap (silent)
                    signalOnDuration = 0L // No active signal
                    delayBeforeNextElement = elementGap // The gap itself is the delay
                }
                'L' -> { // Letter Gap (silent)
                    signalOnDuration = 0L // No active signal
                    delayBeforeNextElement = letterGap // The gap itself is the delay
                }
                'W' -> { // Word Gap (silent, custom duration)
                    signalOnDuration = 0L // No active signal
                    delayBeforeNextElement = CUSTOM_WORD_GAP_MS // Use custom duration for word gap
                }
                'Z' -> { // Custom Loop Gap (silent)
                    signalOnDuration = 0L // No active signal
                    delayBeforeNextElement = CUSTOM_LOOP_GAP_MS // Use custom duration for loop gap
                }
                else -> {
                    Log.w("MorseCodeSignaler", "Unknown Morse element: $element")
                    signalOnDuration = 0L
                    delayBeforeNextElement = 0L
                }
            }

            // Schedule the turn-off of light/sound if it was an active signal (Dot or Dash)
            if (signalOnDuration > 0L) {
                scheduler.schedule({
                    if (isSignaling) {
                        if (useLight) turnFlashlightOff()
                        if (useSound) toneGenerator.stopTone()
                    }
                }, signalOnDuration, TimeUnit.MILLISECONDS)
            }

            // Move to the next element in the sequence
            currentPatternIndex++

            // --- Loop Handling and Scheduling Next Element ---
            val nextScheduledDelay: Long
            if (currentPatternIndex >= currentSignalSequence.size) {
                // End of current sequence, reset for loop, and apply the loop gap
                currentPatternIndex = 0
                // The loop gap was already accounted for by 'Z' element's 'delayBeforeNextElement'
                nextScheduledDelay = delayBeforeNextElement // This will be CUSTOM_LOOP_GAP_MS from 'Z'
            } else {
                // Still within the current sequence, apply the element's specific delay
                nextScheduledDelay = delayBeforeNextElement
            }

            // Schedule the next run of this Runnable after the appropriate delay.
            // THIS IS THE LINE THAT IS NOW CORRECT
            signalFuture = scheduler.schedule(
                this, // 'this' here correctly refers to the 'object : Runnable' instance
                signalOnDuration + nextScheduledDelay, // Total delay before the next signal starts
                TimeUnit.MILLISECONDS
            )
        }
    }


    fun release() {
        stopSignal()
        // Shut down the scheduler completely
        scheduler.shutdownNow()
        // Release the ToneGenerator resources
        toneGenerator.release()
    }
}