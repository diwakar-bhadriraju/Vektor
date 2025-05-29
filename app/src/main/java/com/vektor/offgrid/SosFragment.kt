package com.vektor.offgrid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch

class SosFragment : Fragment() { // Correctly extends Fragment

    private lateinit var morseInputEditText: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var useLightSwitch: MaterialSwitch
    private lateinit var useSoundSwitch: MaterialSwitch
    private lateinit var sosButton: Button

    private lateinit var morseCodeSignaler: MorseCodeSignaler

    // Permission launcher for Camera (flashlight)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Camera permission granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied. Flashlight won't work.", Toast.LENGTH_LONG).show()
            useLightSwitch.isChecked = false // Disable switch if no permission
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements using the 'view' parameter
        morseInputEditText = view.findViewById(R.id.morseInputEditText)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        useLightSwitch = view.findViewById(R.id.useLightSwitch)
        useSoundSwitch = view.findViewById(R.id.useSoundSwitch)
        sosButton = view.findViewById(R.id.sosButton)

        // Initialize MorseCodeSignaler with the Fragment's context
        morseCodeSignaler = MorseCodeSignaler(requireContext())

        // Request Camera permission if not granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Set initial state for switches based on SDK version and hardware
        // Using 'requireContext().packageManager' as it's a Fragment
        if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            useLightSwitch.isEnabled = false // Disable if no flashlight
            useLightSwitch.isChecked = false
            Toast.makeText(requireContext(), "Device has no camera flash.", Toast.LENGTH_LONG).show()
        } else {
            useLightSwitch.isEnabled = true
            useLightSwitch.isChecked = true // Default to on if available
        }

        useSoundSwitch.isChecked = true // Default to on

        startButton.setOnClickListener {
            val inputText = morseInputEditText.text.toString().trim()
            val useLight = useLightSwitch.isChecked
            val useSound = useSoundSwitch.isChecked

            if (inputText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter text for Morse code.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!useLight && !useSound) {
                Toast.makeText(requireContext(), "Select at least one output (light or sound).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Start signal, potentially with a loop and word gap fix
            morseCodeSignaler.startSignal(inputText, useLight, useSound)
        }

        stopButton.setOnClickListener {
            morseCodeSignaler.stopSignal()
        }

        sosButton.setOnClickListener {
            // Fixed SOS message
            morseCodeSignaler.startSignal("SOS", useLightSwitch.isChecked, useSoundSwitch.isChecked)
        }

        // Add TextWatcher to ensure user-entered text is handled properly
        morseInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Stop any ongoing SOS signal if the user starts typing
                morseCodeSignaler.stopSignal()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onPause() {
        super.onPause()
        // Stop any ongoing signal when the fragment is paused (e.g., user navigates away)
        morseCodeSignaler.stopSignal()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release resources when the fragment's view is destroyed
        morseCodeSignaler.release()
    }
}