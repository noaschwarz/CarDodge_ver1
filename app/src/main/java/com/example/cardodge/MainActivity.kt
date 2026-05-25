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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var main_btn_Left: FloatingActionButton
    private lateinit var main_btn_Right: FloatingActionButton
    private lateinit var main_img_hearts: Array<AppCompatImageView>
    private lateinit var lemonMatrixUI: Array<Array<AppCompatImageView>>
    private lateinit var carMatrixUI: Array<AppCompatImageView>

    private val frameDelay: Long = 1000 // lemon speed
    private val gameOverResetDelay: Long = 2000 // delay start after game over
    private lateinit var gameManager: GameManager
    private val gameHandler = Handler(Looper.getMainLooper())
    private lateinit var gameRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViews()
        gameManager = GameManager(main_img_hearts.size)
        initViews()
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
            arrayOf(findViewById(R.id.main_lemon_R0_C0), findViewById(R.id.main_lemon_R0_C1), findViewById(R.id.main_lemon_R0_C2)),
            arrayOf(findViewById(R.id.main_lemon_R1_C0), findViewById(R.id.main_lemon_R1_C1), findViewById(R.id.main_lemon_R1_C2)),
            arrayOf(findViewById(R.id.main_lemon_R2_C0), findViewById(R.id.main_lemon_R2_C1), findViewById(R.id.main_lemon_R2_C2)),
            arrayOf(findViewById(R.id.main_lemon_R3_C0), findViewById(R.id.main_lemon_R3_C1), findViewById(R.id.main_lemon_R3_C2))
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
            gameManager.moveCarLeft()
            refreshRenderUI()
        }
        main_btn_Right.setOnClickListener {
            gameManager.moveCarRight()
            refreshRenderUI()
        }
        refreshRenderUI()
    }

    //our game loop
    private fun setupGameLoop() {
        gameRunnable = object : Runnable {
            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun run() {
                val hitOccurred = gameManager.shiftObstaclesDown() //shift lemons and check hits

                if (hitOccurred) { // if hit let user know
                    handleCrashImpact()
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
        // sync lemon matrix visibility
        for (r in 0 until gameManager.rows) { //for all rows and cols
            for (c in 0 until gameManager.cols) {
                val isActive = gameManager.obstacleMatrix[r][c] //check if you SHOULD see that lemon
                lemonMatrixUI[r][c].visibility = if (isActive) View.VISIBLE else View.INVISIBLE // if yeah, make it show
            }
        }
        // sync car lane
        for (c in 0 until gameManager.cols) { //go over all the lanes and see where we are
            carMatrixUI[c].visibility = if (c == gameManager.currentCarLane) View.VISIBLE else View.INVISIBLE
        }
    }

    // hearts visibility
    private fun updateHeartsUI() {
        val totalHits = gameManager.numberOfHits //check num of hits
        if (totalHits in 1..main_img_hearts.size) { // ensure the num of hearts we see is correct
            main_img_hearts[main_img_hearts.size - totalHits].visibility = View.INVISIBLE
        }
    }

    // handle game over
    private fun handleGameOver() {
        Toast.makeText(this, "Game Over! don't cry over spilled lemonade...", Toast.LENGTH_LONG).show()
        gameManager.reset() //reset all locations

        for (heart in main_img_hearts) { //reset hearts
            heart.visibility = View.VISIBLE
        }
        refreshRenderUI()
        gameHandler.postDelayed(gameRunnable, gameOverResetDelay)
    }

    // vibration
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //new ver so run in new way
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") //older ver so run old way
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(300)
        }
    }

    override fun onResume() {
        super.onResume()
        gameHandler.postDelayed(gameRunnable, frameDelay)
    }

    override fun onPause() {
        super.onPause()
        gameHandler.removeCallbacks(gameRunnable)
    }
}