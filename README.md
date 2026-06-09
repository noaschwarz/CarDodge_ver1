# CarDodge

CarDodge is an arcade pixel style lane-dodging game built natively for Android in Kotlin. 
Players navigate a vehicle across a 5-lane grid highway to dodge oncoming obstacles (lemons) while collecting coins and counting distance traveled. 
The project serves as an implementation study of real-time rendering loops, mobile sensor hardware integration, local persistence management, and geographical tracking APIs.

# The Core Feature Architecture

The application is structured around three primary gameplay milestones and layout frameworks:

# 1. Dual Control Configurations
The game adapts to different user access modes toggled natively via the Main Menu:
* **Button Interaction Mode:** Standard digital buttons (`FloatingActionButton`) map lane shifting via manual layout click listener actions.
* **Tilt Sensor Mode:** Registers real-time lateral acceleration variants via the device's internal physical `Hardware.SENSOR_TYPE_ACCELEROMETER` stream, allowing hands-free navigation.

# 2. Sequential Game Over Overlay & Data Synchronization
To eliminate layout stacking issues and trailing background tasks, the game-over sequence fires in a strictly regulated handoff order:
1.  **Halt:** The gameplay loop handles `removeCallbacksAndMessages(null)` to permanently terminate lingering game runnables and background toasts.
2.  **Locate:** The system safely pings the `FusedLocationProviderClient` for precise GPS coordinates.
3.  **Prompt:** An un-cancelable input alert captures player registration credentials.
4.  **Isolate:** The full-screen `main_layout_game_over` state is cleanly exposed without visual artifacts or overlap.

# 3. High Score Fragment Dashboard with Split-Screen Google Maps
An analytical split-screen dashboard layout displays a structural overview of the top 10 player records alongside a live interactive mapping canvas:
* **Top Half (`ScoreListFragment`):** Contains a fixed-bound `RecyclerView` with optimized view caching (`findViewById` isolation) that lists player ranks, timestamps, and compact data cells.
* **Bottom Half (`SupportMapFragment`):** Uses an un-casted asynchronous interface handshake (`OnScoreClickListener`). Clicking any historical record row triggers a high-accuracy map camera animation that drops a placement marker on the exact coordinate plane where that collision happened.


# Technical Stack & Frameworks

* **Language:** Kotlin 1.9+ (Structured Concurrency, Object-Oriented Frameworks)
* **Minimum Android SDK Supported:** API Level 24 (Android 7.0 Nougat)
* **Target Android SDK Compatibility:** API Level 34 (Android 14)
* **Asynchronous Engine:** `android.os.Handler` paired with looping UI main `Looper` runnables.
* **Hardware Sensors:** `SensorManager` & `SensorEventListener` for physics-based tilt processing.
* **Location Services:** Google Play Services Location SDK (`FusedLocationProviderClient`) utilizing dynamic fallback overrides (`getCurrentLocation` verification pipelines).
* **Mapping Interface:** Google Play Services Maps SDK v19.0.0.
* **Persistence Layer:** `SharedPreferences` managing key-value serialization arrays parsed through a Google `Gson` schema.


# System Architecture Overview

Below is an overview of the key file locations across the modular project architecture:

```text
CarDodge/
├── app/
│   ├── src/main/java/com/example/cardodge/
│   │   ├── MainActivity.kt        # Dedicated game-loop manager, UI refresh engine & sensor tracker
│   │   ├── MenuActivity.kt        # Primary navigation console, control toggles & speed settings
│   │   ├── HighScoreActivity.kt   # Split-screen host implementing communication interface locks
│   │   └── utilities/
│   │       ├── GameManager.kt     # Logical core handling coordinate matrix shifting & life calculations
│   │       ├── ScoreManager.kt    # SharedPreferences database controller handling top 10 filtering
│   │       ├── ScoreRecord.kt     # Immutable data structure logging name, score, timestamp, and location
│   │       └── ScoreListFragment.kt # RecyclerView adapter mapping custom score rows
│   └── src/main/res/
│       ├── layout/                # UI definitions (activity_main, activity_menu, activity_high_score)
│       └── raw/                   # Game audio resources (meep_meep.mp3)
└── local.properties               # Encrypted local configuration keys
