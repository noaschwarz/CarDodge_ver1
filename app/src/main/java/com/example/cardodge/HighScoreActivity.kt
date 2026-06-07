package com.example.cardodge

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.cardodge.utilities.ScoreListFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.Button

class HighScoreActivity : AppCompatActivity(), ScoreListFragment.OnScoreClickListener, OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { renderer ->
            when (renderer) {
                MapsInitializer.Renderer.LATEST -> Log.d("MapsInit", "The latest renderer is loaded successfully.")
                MapsInitializer.Renderer.LEGACY -> Log.d("MapsInit", "The legacy renderer is loaded.")
            }
        }

        setContentView(R.layout.activity_high_score)
        val btnBack = findViewById<Button>(R.id.high_score_BTN_back)
        btnBack.setOnClickListener {
            finish()
        }

        val listFragment = supportFragmentManager.findFragmentById(R.id.high_score_FRAME_list) as? ScoreListFragment
        listFragment?.setOnScoreClickListener(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.high_score_FRAME_map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map

        val defaultLocation = LatLng(35.3606, 138.7274) //fuji
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 2.0f))
    }

    override fun onScoreClicked(latitude: Double, longitude: Double) {
        val targetLocation = LatLng(latitude, longitude)

        googleMap?.let { map ->
            map.clear()
            // marker exactly where the high score was set
            map.addMarker(
                MarkerOptions()
                    .position(targetLocation)
                    .title("Game Record Location")
            )

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 14.0f))
        }
    }
}