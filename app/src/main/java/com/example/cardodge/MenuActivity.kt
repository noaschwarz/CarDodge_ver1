package com.example.cardodge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

class MenuActivity : AppCompatActivity() {

    private var useSensorMode = false
    private var selectedDelay = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val btnControlMode = findViewById<Button>(R.id.menu_btn_control_mode)
        val btnStartGame = findViewById<Button>(R.id.menu_btn_start_game)
        val btnSlow = findViewById<Button>(R.id.menu_btn_buttons_slow)
        val btnFast = findViewById<Button>(R.id.menu_btn_buttons_fast)
        val btnHighScores = findViewById<Button>(R.id.menu_btn_high_scores)
        val colorSelected = ContextCompat.getColorStateList(this, R.color.gray)
        val colorUnselected = ContextCompat.getColorStateList(this, R.color.dark_light_blue)
        btnControlMode.text = "Current Control Mode: Buttons" //default
        btnSlow.backgroundTintList = colorUnselected //default
        btnFast.backgroundTintList = colorSelected //default

        btnControlMode.setOnClickListener {
            useSensorMode = !useSensorMode //flip mode and text
            if (useSensorMode) {
                btnControlMode.text = "Current Control Mode: Tilt (Sensor)"
            } else {
                btnControlMode.text = "Current Control Mode: Buttons"
            }
        }

        //change speed based on need
        btnFast.setOnClickListener {
            selectedDelay = 1000L
            btnSlow.backgroundTintList = colorUnselected
            btnFast.backgroundTintList = colorSelected
        }
        btnSlow.setOnClickListener {
            selectedDelay = 500L
            btnSlow.backgroundTintList = colorSelected
            btnFast.backgroundTintList = colorUnselected
        }

        btnStartGame.setOnClickListener {
            startGame(useSensor = useSensorMode, delay = selectedDelay)
        }

        btnHighScores.setOnClickListener {
            val intent = Intent(this, HighScoreActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startGame(useSensor: Boolean, delay: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("EXTRA_USE_SENSOR", useSensor)
            putExtra("EXTRA_FRAME_DELAY", delay)
        }
        startActivity(intent)
    }
}