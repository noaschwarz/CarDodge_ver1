package com.example.cardodge.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.cardodge.R

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) { //create our menu
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        findViewById<Button>(R.id.menu_btn_buttons_slow).setOnClickListener {
            startGame(useSensor = false, delay = 1000L)
        }
        findViewById<Button>(R.id.menu_btn_buttons_fast).setOnClickListener {
            startGame(useSensor = false, delay = 500L)
        }
        findViewById<Button>(R.id.menu_btn_sensor).setOnClickListener {
            startGame(useSensor = true, delay = 600L)
        }
        findViewById<Button>(R.id.menu_btn_high_scores).setOnClickListener {
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