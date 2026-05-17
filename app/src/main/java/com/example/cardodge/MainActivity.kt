package com.example.cardodge

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var mainLayout: RelativeLayout
    private lateinit var main_img_car: AppCompatImageView
    private lateinit var main_btn_Left: FloatingActionButton
    private lateinit var main_btn_Right: FloatingActionButton
    private lateinit var main_img_hearts: Array<AppCompatImageView>

    private var currLane = 1         // 0 = Left, 1 = Middle, 2 = Right
    private val dropSpeed = 15f         // How many pixels the lemon drops per frame
    private val frameDelay: Long = 30   // Game tick delay in milliseconds

    private val activeLemons = ArrayList<LemonObstacle>()
    private val MaxLemons = 3
    private var spawnTimeCounter = 0
    private val SpawnWaitTime = 45 //around 1.3 sec

    private lateinit var gameManager: GameManager
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        gameManager = GameManager(main_img_hearts.size)
        initViews()
        setupGameLoop()
    }

    private fun findViews() {
        mainLayout = findViewById(R.id.main_layout)
        main_img_car = findViewById(R.id.main_IMG_car)
        main_btn_Left = findViewById(R.id.main_FAB_left)
        main_btn_Right = findViewById(R.id.main_FAB_right)
        main_img_hearts = arrayOf(
            findViewById(R.id.main_img_heart0),
            findViewById(R.id.main_img_heart1),
            findViewById(R.id.main_img_heart2),
        )
    }

    private fun initViews() {
        main_btn_Left.setOnClickListener { moveCarLeft() }
        main_btn_Right.setOnClickListener { moveCarRight() }

        main_img_car.post {
            updateCarPosition()
        }
    }

    override fun onResume() {
        super.onResume()
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()
        stopGameLoop()
    }

    private fun moveCarLeft() {
        if (currLane > 0){
            currLane--
            updateCarPosition()
        }
    }

    private fun moveCarRight() {
        if (currLane < 2){
            currLane++
            updateCarPosition()
        }
    }

    private fun updateCarPosition() {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val laneWidth = screenWidth / 3f
        val targetX = (currLane * laneWidth) + (laneWidth / 2f) - (main_img_car.width / 2f)
        main_img_car.x = targetX
    }

    private fun setupGameLoop() {
        gameRunnable = object : Runnable {
            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun run() {
                spawnTimeCounter++
                if (spawnTimeCounter >= SpawnWaitTime && activeLemons.size < MaxLemons){
                    spawnNewLemon()
                    spawnTimeCounter = 0
                }

                moveObstacleDown()
                checkCollision()

                gameHandler.postDelayed(this, frameDelay)
            }
        }
    }

    private fun spawnNewLemon() {
        val newLemonView = AppCompatImageView(this)
        newLemonView.setImageResource(R.drawable.pixel_lemon)

        val carSizepx = resources.getDimensionPixelSize(R.dimen.car_size)
        val layoutParams = RelativeLayout.LayoutParams(carSizepx, carSizepx)
        newLemonView.layoutParams = layoutParams

        newLemonView.y = -carSizepx.toFloat() - 100f
        val chosenLane = Random.nextInt(3)
        mainLayout.addView(newLemonView)

        val LemonObstacle = LemonObstacle(newLemonView, chosenLane)
        activeLemons.add(LemonObstacle)

        newLemonView.post {
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val laneWidth = screenWidth / 3f
            newLemonView.x = (chosenLane * laneWidth) + (laneWidth / 2f) - (newLemonView.width / 2f)
        }

    }

    private fun moveObstacleDown() {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val iterator = activeLemons.iterator()

        while (iterator.hasNext()) {
            val lemon = iterator.next()
            lemon.view.y += dropSpeed

            if (lemon.view.y > screenHeight) {
                resetObstacle(lemon)
            }
        }
    }

    private fun resetObstacle(lemon: LemonObstacle) {
        lemon.view.y = -lemon.view.height.toFloat() - 100f
        lemon.lane = Random.nextInt(3)

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val laneWidth = screenWidth / 3f
        lemon.view.x = (lemon.lane * laneWidth) + (laneWidth / 2f) - (lemon.view.width / 2f)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun checkCollision() {
        val carTop = main_img_car.y
        val carBottom = main_img_car.y + main_img_car.height

        for (lemon in activeLemons) {
            if (currLane == lemon.lane) {
                val lemonBottom = lemon.view.y + lemon.view.height
                val lemonTop = lemon.view.y

                if (lemonBottom >= carTop && lemonTop <= carBottom) {
                    handleCrash(lemon)
                    break // Exit early to avoid compound processing loops in a single frame tick
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun handleCrash(hitLemon: LemonObstacle) {
        gameManager.registerHit()

        Toast.makeText(this, "Crash! 💥", Toast.LENGTH_SHORT).show()
        triggerVibration()
        updateLivesUI()

        resetObstacle(hitLemon)

        if (gameManager.isGameOver) {
            restartGame()
        }
    }

    private fun updateLivesUI() {
        val hits = gameManager.numberOfHits
        if (hits in 1..main_img_hearts.size) {
            main_img_hearts[main_img_hearts.size - hits].visibility = View.INVISIBLE
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(300)
        }
    }

    private fun restartGame() {
        Toast.makeText(this, "Game Over! Restarting...", Toast.LENGTH_LONG).show()
        gameManager.reset()
        currLane = 1
        updateCarPosition()

        for (heart in main_img_hearts) {
            heart.visibility = View.VISIBLE
        }

        for (lemon in activeLemons) {
            mainLayout.removeView(lemon.view)
        }
        activeLemons.clear()
        spawnTimeCounter = 0
    }

    private fun startGameLoop() {
        gameHandler.postDelayed(gameRunnable, frameDelay)
    }

    private fun stopGameLoop() {
        gameHandler.removeCallbacks(gameRunnable)
    }
}