package com.example.cardodge.activities

import android.Manifest
import android.annotation.SuppressLint
import android.hardware.SensorEvent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.example.cardodge.R
import com.example.cardodge.managers.GameManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var main_btn_Left: FloatingActionButton
    private lateinit var main_btn_Right: FloatingActionButton
    private lateinit var main_img_hearts: Array<AppCompatImageView>
    private lateinit var lemonMatrixUI: Array<Array<AppCompatImageView>>
    private lateinit var carMatrixUI: Array<AppCompatImageView>
    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient

    private var frameDelay: Long = 1000L // items speed
    private val gameOverResetDelay: Long = 4000 // delay start after game over
    private var useSensorMode: Boolean = false // buttons or movement based controls
    private lateinit var gameManager: GameManager
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable
    private var isResetting = false // check for reset
    private var accelerometer: Sensor? = null
    private var lastLatitude: Double = 35.3606  // defaul
    private var lastLongitude: Double = 138.7274 // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        useSensorMode = intent.getBooleanExtra("EXTRA_USE_SENSOR", false)
        frameDelay = intent.getLongExtra("EXTRA_FRAME_DELAY", 1000L)
        requestLocationPermissions()

        findViews()
        gameManager = GameManager(main_img_hearts.size)
        initViews()
        if (useSensorMode) { // see what mode we are and set up whats needed
            main_btn_Left.visibility = View.GONE
            main_btn_Right.visibility = View.GONE
            initSensor()
        } else {
            main_btn_Left.visibility = View.VISIBLE
            main_btn_Right.visibility = View.VISIBLE
        }
        setupGameLoop()
    }

    private fun findViews() {
        main_btn_Left = findViewById(R.id.main_FAB_left)
        main_btn_Right = findViewById(R.id.main_FAB_right)

        main_img_hearts = arrayOf(
            findViewById(R.id.main_img_heart0),
            findViewById(R.id.main_img_heart1),
            findViewById(R.id.main_img_heart2)
        )

        // map out the lemons in our matrix
        lemonMatrixUI = arrayOf(
            arrayOf(findViewById(R.id.main_lemon_R0_C0), findViewById(R.id.main_lemon_R0_C1), findViewById(
                R.id.main_lemon_R0_C2)),
            arrayOf(findViewById(R.id.main_lemon_R1_C0), findViewById(R.id.main_lemon_R1_C1), findViewById(
                R.id.main_lemon_R1_C2)),
            arrayOf(findViewById(R.id.main_lemon_R2_C0), findViewById(R.id.main_lemon_R2_C1), findViewById(
                R.id.main_lemon_R2_C2)),
            arrayOf(findViewById(R.id.main_lemon_R3_C0), findViewById(R.id.main_lemon_R3_C1), findViewById(
                R.id.main_lemon_R3_C2))
        )

        // map out car locations
        carMatrixUI = arrayOf(
            findViewById(R.id.main_car_C0),
            findViewById(R.id.main_car_C1),
            findViewById(R.id.main_car_C2)
        )
    }

    private fun initViews() {
        main_btn_Left.setOnClickListener {
            if (!isResetting) { // move if not resetting
                gameManager.moveCarLeft()
                refreshRenderUI()
            }
        }
        main_btn_Right.setOnClickListener {
            if (!isResetting) { // move if not resetting
                gameManager.moveCarRight()
                refreshRenderUI()
            }
        }
        refreshRenderUI()
    }

    //our game loop
    private fun setupGameLoop() {
        gameRunnable = object : Runnable {
            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun run() {
                if (isResetting) return
                val hitOccurred = gameManager.shiftObstaclesDown() //shift lemons and check hits

                if (hitResult == 1) { // if we are hit by lemon, handle (life loss, vibration, toast)
                    handleCrashImpact()
                } else if (hitResult == 2) { // if catch a coin, handle (points, noise)
                    Toast.makeText(this, "+5 Coin Collected!", Toast.LENGTH_SHORT).show()
                }

                refreshRenderUI()

                if (!gameManager.isGameOver) { //if not over continue
                    gameHandler.postDelayed(this, frameDelay)
                }
            }
        }
    }

    // handle a crash, send out text and vibrate
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun handleCrashImpact() {
        Toast.makeText(this, "Sour! you crashed", Toast.LENGTH_SHORT).show()
        triggerVibration()
        gameManager.clearCurrentCollisionObstacle()
        updateHeartsUI()
        if (gameManager.isGameOver) { // if hits == lives -> game over
            handleGameOver()
        }
    }

    //refresh the view after every change
    private fun refreshRenderUI() {
        // sync itmes matrix visibility
        for (r in 0 until gameManager.rows) { //for all rows and cols
            for (c in 0 until gameManager.cols) {
                val itemType = gameManager.obstacleMatrix[r][c] //check if you SHOULD see that item

                when (itemType) { //check what our item type based on the generateNewRowItems drop rate
                    1 -> { //lemon
                        lemonMatrixUI[r][c].visibility = View.VISIBLE
                        lemonMatrixUI[r][c].setImageResource(R.drawable.pixel_lemon)
                    }
                    2 -> { //coin
                        lemonMatrixUI[r][c].visibility = View.VISIBLE
                        lemonMatrixUI[r][c].setImageResource(R.drawable.pixel_coin)
                    }
                    else -> {
                        lemonMatrixUI[r][c].visibility = View.INVISIBLE
                    }
                }
            }
        }
        // odometer String metrics
        val totalScoreDisplay = gameManager.distanceOdometer + gameManager.coinScore
        findViewById<TextView>(R.id.main_lbl_odometer).text = String.format("%05d m", totalScoreDisplay)
        // sync car lane
        for (c in 0 until gameManager.cols) { //go over all the lanes and see where we are
            carMatrixUI[c].visibility = if (c == gameManager.currentCarLane) View.VISIBLE else View.INVISIBLE
        }
    }

    // hearts visibility
    private fun updateHeartsUI() {
        val totalHits = gameManager.numberOfHits // check num of hits
        if (totalHits in 1..main_img_hearts.size) { // ensure the num of hearts we see is correct
            main_img_hearts[totalHits - 1].visibility = View.INVISIBLE
        }
    }

    private fun handleGameOver() {
        //check if restart is happening
        isResetting = true
        gameHandler.removeCallbacks(gameRunnable)
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                }
                showHighScoreInputDialog()
            }.addOnFailureListener {
                showHighScoreInputDialog() // default if failes
            }
        } else {
            showHighScoreInputDialog() // default if no permission
        }
    }

    private fun showHighScoreInputDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Game Over!")

        val totalScore = gameManager.distanceOdometer + gameManager.coinScore
        builder.setMessage("Final Score: $totalScore m\nEnter your name:")

        val input = android.widget.EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val name = input.text.toString().ifEmpty { "Player" }

            val record = ScoreRecord(
                playerName = name,
                score = totalScore,
                timestamp = System.currentTimeMillis(),
                latitude = lastLatitude,
                longitude = lastLongitude
            )
            HighScoreManager(this).addScore(record)
            executeRestartSequence()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun executeRestartSequence() {
        Toast.makeText(this, "don't cry over spilled lemonade...", Toast.LENGTH_LONG).show()
        gameManager.reset()

        for (heart in main_img_hearts) {
            heart.visibility = View.VISIBLE
        }
        refreshRenderUI()

        gameHandler.postDelayed({
            isResetting = false
            gameHandler.postDelayed(gameRunnable, frameDelay)
        }, gameOverResetDelay)
    }

    // vibration
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //new ver so run in new way
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") //older ver so run old way
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(300)
        }
    }

    @SuppressLint("ServiceCast")
    private fun initSensor() { //start our sensor for movement based functionality
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val sensorEventListener = object : SensorEventListener { //listener for movement
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            val x = event.values[0] // Lateral tilt

            if (x < -2.0) { // Tilt Right
                gameManager.moveCarRight()
                refreshRenderUI()
            } else if (x > 2.0) { // Tilt Left
                gameManager.moveCarLeft()
                refreshRenderUI()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        androidx.core.app.ActivityCompat.requestPermissions(this, permissions, 100)
    }

    override fun onResume() {
        super.onResume()
        if (!isResetting) {
            gameHandler.removeCallbacks(gameRunnable)
            gameHandler.postDelayed(gameRunnable, frameDelay)
        }
    }

    override fun onPause() {
        super.onPause()
        gameHandler.removeCallbacks(gameRunnable)
    }
}