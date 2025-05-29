# 🌍 Vektor: Your Essential Wilderness Companion

## ✨ About Vektor

Welcome to **Vektor**! This Android application is your ultimate tool for survival and navigation when you're off the beaten path and disconnected from modern conveniences. Designed with adventurers, outdoor enthusiasts, and preppers in mind, Vektor provides critical information and utility features directly from your device's sensors and carefully selected APIs, even when internet access is limited.

Whether you're planning a hike, exploring remote areas, or preparing for emergencies, Vektor aims to equip you with the knowledge and tools to stay safe and informed.

---

## 🚀 Features

Vektor is under active development, constantly adding more essential tools to its arsenal. Here's a look at what's already implemented and what's coming soon!

### ✅ Implemented Features

These features are currently available and ready for use in the app:

* **🌅 Sunrise & Sunset Tracker:**
    * Get precise local sunrise and sunset times for your current location.
    * Essential for planning daily activities and managing daylight hours.
    * Includes a robust **internet connectivity check** to inform you if data can't be fetched.
* **☀️ Weather Snapshot:**
    * View current weather conditions for your location, including temperature (°C), humidity (%), wind speed (m/s), and a brief description (e.g., "clear sky," "light rain").
    * Leverages the OpenWeatherMap API for up-to-date information.
    * Also features an **internet connectivity check** for reliable operation.
* **🧲 Magnetic Field Strength Detector:**
    * Utilizes your device's magnetometer to display real-time magnetic field strength along the X, Y, and Z axes, and calculates the total magnitude (in µT).
    * Useful for detecting magnetic anomalies or calibrating compasses.
* **🧭 Compass & Altitude:**
    * Provides a digital compass for orientation.
    * Displays approximate altitude, aiding in navigation and understanding terrain.
* **🚨 Morse Code Signal Generator (SOS):**
    * A simple yet powerful tool to generate SOS signals using your device's flashlight.
    * Crucial for emergency signaling when other communication methods fail.

---

### ⏳ Features Under Development (Coming Soon!)

I'm constantly working to expand Vektor's capabilities. Here's a peek at what's next on our roadmap:

* **📍 GPS Path Tracking:** Track and record your movement path, useful for backtracking or mapping your journey.
* **🗺️ Offline Map & Waypoints:** Download maps for offline use and mark important waypoints, ensuring navigation without an internet connection.
* **🔕 Silent SOS Trigger:** A discreet way to send out emergency alerts without drawing attention.
* **🚨 Intrusion Detection:** Utilize device sensors to detect movement or disturbances around your camp or belongings.
* **👻 Stealth Mode:** Optimize device settings to minimize detectable electronic signatures.
* **🔋 Battery Saver:** Intelligent modes to extend your device's battery life in critical situations.
* **📊 Sensor Check:** A comprehensive diagnostic tool for all your device's available sensors.
* **🚶 Step Counter:** Keep track of your daily steps, useful for monitoring activity and estimating distances.
* **📚 Offline Survival Guide:** Access essential survival tips and information without an internet connection.
* **📡 Bluetooth Device Scanner:** Scan for nearby Bluetooth devices, potentially locating other people or equipment.
* **💬 P2P Messaging:** Explore peer-to-peer communication methods for short-range messaging without cellular networks.

---

## 🛠️ Technologies Used

Vektor is built using modern Android development practices and technologies:

* **Kotlin:** The primary programming language for Android application development.
* **Android SDK & Jetpack Components:** Leveraging the latest Android APIs and architectural components for robust and maintainable code.
* **Kotlin Coroutines:** For asynchronous operations and efficient network requests, ensuring a smooth user experience.
* **Google Location Services API:** For accurate and efficient location determination.
* **OpenWeatherMap API:** For current weather data.
* `SimpleDateFormat` & `TimeZone`: For handling and converting time data.
* `SensorManager`: For accessing device sensors like the magnetometer.

---

## 🚀 Getting Started

To get a copy of the project up and running on your local machine for development and testing purposes, follow these steps.

### Prerequisites

* Android Studio (latest version recommended)
* An Android device or emulator running Android 6.0 (Marshmallow) or higher.
* An **OpenWeatherMap API Key** (for the Weather Snapshot feature). You can get one for free by signing up at [OpenWeatherMap API](https://openweathermap.org/api).

### Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/YOUR_GITHUB_USERNAME/Vektor.git](https://github.com/diwakar-bhadriraju/Vektor.git)
    cd Vektor
    ```
    (Replace `YOUR_GITHUB_USERNAME` with your actual GitHub username, and ensure `Vektor.git` matches your repository name if you've renamed it on GitHub.)

2.  **Open in Android Studio:**
    * Launch Android Studio.
    * Select `File` > `Open` and navigate to the cloned `Vektor` directory.

3.  **Sync Project with Gradle Files:**
    * Android Studio should automatically prompt you to sync the project. If not, click on the "Sync Project with Gradle Files" icon (a refresh icon with an elephant).

4.  **Add OpenWeatherMap API Key:**
    * Open `app/src/main/java/com/vektor/offgrid/WeatherFragment.kt`.
    * Locate the line:
        ```kotlin
        private val OPEN_WEATHER_MAP_API_KEY = "YOUR_OPENWEATHERMAP_API_KEY" // <--- REPLACE THIS
        ```
    * Replace `"YOUR_OPENWEATHERMAP_API_KEY"` with your actual API key obtained from OpenWeatherMap.

5.  **Run the App:**
    * Connect an Android device to your computer or launch an emulator.
    * Click the `Run 'app'` button (green play icon) in Android Studio.

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! ⭐ Thanks again!

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information. (You might need to create a `LICENSE` file in your root directory if you haven't already).

---

## 📧 Contact

Your Name - [Your GitHub Profile Link](https://github.com/diwakar-bhadriraju)
Project Link: [https://github.com/YOUR_GITHUB_USERNAME/Vektor](https://github.com/diwakar-bhadriraju/Vektor)

---
