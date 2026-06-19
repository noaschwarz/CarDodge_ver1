package com.example.cardodge

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.hardware.SensorEvent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import com.example.cardodge.utilities.GameManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.media.SoundPool
import android.widget.Button
import android.widget.TextView
import com.example.cardodge.utilities.ScoreManager
import com.example.cardodge.utilities.ScoreRecord

class MainActivity : AppCompatActivity() {

    private lateinit var main_btn_Left: FloatingActionButton
    private lateinit var main_btn_Right: FloatingActionButton
    private lateinit var main_img_hearts: Array<AppCompatImageView>
    private lateinit var itemMatrixUI: Array<Array<AppCompatImageView>>
    private lateinit var carMatrixUI: Array<AppCompatImageView>
    private lateinit var sensorManager: SensorManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mainLayoutGameOver: View
    private lateinit var endLblDistance: TextView
    private lateinit var endLblCoins: TextView
    private lateinit var endBtnPlayAgain: Button
    private lateinit var endBtnOpenMenu: Button
    private lateinit var mainLblOdometer: TextView

    private var frameDelay: Long = 1000L // items speed
    private val gameOverResetDelay: Long = 4000 // delay start after game over
    private var useSensorMode: Boolean = false // buttons or movement based controls
    private lateinit var gameManager: GameManager
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable
    private var isResetting = false // check for reset
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var lastTiltTime: Long = 0L // cooldown timer flag for tilt processing
    private var lastLatitude: Double = 35.3606  // defaul
    private var lastLongitude: Double = 138.7274 // default
    private lateinit var soundPool: SoundPool
    private var lemonSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .build()
        lemonSoundId = soundPool.load(this, R.raw.meep_meep, 1)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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
        gameHandler.postDelayed(gameRunnable, frameDelay)
    }

    private fun findViews() {
        main_btn_Left = findViewById(R.id.main_FAB_left)
        main_btn_Right = findViewById(R.id.main_FAB_right)

        main_img_hearts = arrayOf(
            findViewById(R.id.main_img_heart0),
            findViewById(R.id.main_img_heart1),
            findViewById(R.id.main_img_heart2)
        )

        // map out all items in our matrix
        itemMatrixUI = arrayOf(
            arrayOf(findViewById(R.id.main_lemon_R0_C0), findViewById(R.id.main_lemon_R0_C1), findViewById(R.id.main_lemon_R0_C2), findViewById(R.id.main_lemon_R0_C3), findViewById(R.id.main_lemon_R0_C4)),
            arrayOf(findViewById(R.id.main_lemon_R1_C0), findViewById(R.id.main_lemon_R1_C1), findViewById(R.id.main_lemon_R1_C2), findViewById(R.id.main_lemon_R1_C3), findViewById(R.id.main_lemon_R1_C4)),
            arrayOf(findViewById(R.id.main_lemon_R2_C0), findViewById(R.id.main_lemon_R2_C1), findViewById(R.id.main_lemon_R2_C2), findViewById(R.id.main_lemon_R2_C3), findViewById(R.id.main_lemon_R2_C4)),
            arrayOf(findViewById(R.id.main_lemon_R3_C0), findViewById(R.id.main_lemon_R3_C1), findViewById(R.id.main_lemon_R3_C2), findViewById(R.id.main_lemon_R3_C3), findViewById(R.id.main_lemon_R3_C4)),
            arrayOf(findViewById(R.id.main_lemon_R4_C0), findViewById(R.id.main_lemon_R4_C1), findViewById(R.id.main_lemon_R4_C2), findViewById(R.id.main_lemon_R4_C3), findViewById(R.id.main_lemon_R4_C4)),
            arrayOf(findViewById(R.id.main_lemon_R5_C0), findViewById(R.id.main_lemon_R5_C1), findViewById(R.id.main_lemon_R5_C2), findViewById(R.id.main_lemon_R5_C3), findViewById(R.id.main_lemon_R5_C4))
        )

        // map out car locations
        carMatrixUI = arrayOf(
            findViewById(R.id.main_car_C0),
            findViewById(R.id.main_car_C1),
            findViewById(R.id.main_car_C2),
            findViewById(R.id.main_car_C3),
            findViewById(R.id.main_car_C4)
        )

        mainLayoutGameOver = findViewById(R.id.main_layout_game_over)
        endLblDistance = findViewById(R.id.end_lbl_distance)
        endLblCoins = findViewById(R.id.end_lbl_coins)
        endBtnPlayAgain = findViewById<Button>(R.id.end_btn_play_again)
        endBtnOpenMenu = findViewById<Button>(R.id.end_btn_open_menu)
        mainLblOdometer = findViewById(R.id.main_lbl_odometer)
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
                val hitResult = gameManager.shiftObstaclesDown() //shift lemons and check hits

                if (hitResult == 1) { // if we are hit by lemon, handle (life loss, vibration, toast)
                    handleCrashImpact()
                } else if (hitResult == 2) { // if catch a coin, handle (points, noise)
                    Toast.makeText(this@MainActivity, "+5 Coin Collected!", Toast.LENGTH_SHORT).show()
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
        if (lemonSoundId != 0) {
            soundPool.play(lemonSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
        triggerVibration()
        gameManager.clearCurrentCollisionObstacle()
        updateHeartsUI()
        if (gameManager.isGameOver) { // if hits == lives -> game over
            handleGameOver()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundPool.isInitialized) {
            soundPool.release()
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
                        itemMatrixUI[r][c].visibility = View.VISIBLE
                        itemMatrixUI[r][c].setImageResource(R.drawable.pixel_lemon)
                    }
                    2 -> { //coin
                        itemMatrixUI[r][c].visibility = View.VISIBLE
                        itemMatrixUI[r][c].setImageResource(R.drawable.pixel_coin)
                    }
                    else -> {
                        itemMatrixUI[r][c].visibility = View.INVISIBLE
                    }
                }
            }
        }
        // odometer String metrics
        // odometer String metrics
        val totalScoreDisplay = gameManager.distanceOdometer + gameManager.coinScore
        mainLblOdometer.text = String.format("%05d m", totalScoreDisplay)
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
        isResetting = true
        gameHandler.removeCallbacks(gameRunnable)
        gameHandler.removeCallbacksAndMessages(null)

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                    showHighScoreInputDialog()
                } else {
                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { freshLocation ->
                        if (freshLocation != null) {
                            lastLatitude = freshLocation.latitude
                            lastLongitude = freshLocation.longitude
                        }
                        showHighScoreInputDialog()
                    }.addOnFailureListener {
                        showHighScoreInputDialog()
                    }
                }
            }.addOnFailureListener {
                showHighScoreInputDialog()
            }
        } else {
            showHighScoreInputDialog()
        }
    }

    private fun showHighScoreInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Over!")

        val totalScore = gameManager.distanceOdometer + gameManager.coinScore
        builder.setMessage("Final Score: $totalScore m\nEnter your name:")

        val input = EditText(this)
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
            ScoreManager(this).addScore(record)
            showEndGameScreenLayout()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showEndGameScreenLayout() {
        endLblDistance.text = "Distance Traveled: ${gameManager.distanceOdometer} m"
        endLblCoins.text = "Coins Collected: ${gameManager.coinScore}"

        mainLayoutGameOver.visibility = View.VISIBLE
        main_btn_Left.visibility = View.GONE
        main_btn_Right.visibility = View.GONE

        endBtnPlayAgain.setOnClickListener {
            mainLayoutGameOver.visibility = View.GONE
            executeRestartSequence()
        }

        endBtnOpenMenu.setOnClickListener {
            finish()
        }
    }

    private fun executeRestartSequence() {
        Toast.makeText(this, "restating, so don't cry over spilled lemonade", Toast.LENGTH_LONG).show()
        gameManager.reset()
        if (useSensorMode) {
            main_btn_Left.visibility = View.GONE
            main_btn_Right.visibility = View.GONE
        } else {
            main_btn_Left.visibility = View.VISIBLE
            main_btn_Right.visibility = View.VISIBLE
        }
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
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") //older ver so run old way
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(300)
        }
    }

    @SuppressLint("ServiceCast")
    private fun initSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || isResetting) return

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val accX = event.values[0]
                val currentTime = System.currentTimeMillis()
                val DEADBONE_THRESHOLD = 2.0f
                if (currentTime - lastTiltTime > 250L) {
                    if (accX > DEADBONE_THRESHOLD) {
                        gameManager.moveCarLeft()
                        refreshRenderUI()
                        lastTiltTime = currentTime
                    }
                    else if (accX < -DEADBONE_THRESHOLD) {
                        gameManager.moveCarRight()
                        refreshRenderUI()
                        lastTiltTime = currentTime
                    }
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this, permissions, 100)
    }

    override fun onResume() {
        super.onResume()
        if (useSensorMode) {
            accelerometer?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME) }
        }
        if (!isResetting && ::gameRunnable.isInitialized) {
            gameHandler.removeCallbacks(gameRunnable)
            gameHandler.postDelayed(gameRunnable, frameDelay)
        }
    }

    override fun onPause() {
        super.onPause()
        if (useSensorMode) {
            sensorManager.unregisterListener(sensorEventListener)
        }
        gameHandler.removeCallbacks(gameRunnable)
    }
}