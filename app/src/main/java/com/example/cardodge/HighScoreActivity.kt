package com.example.cardodge

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.cardodge.utilities.ScoreListFragment
import com.example.cardodge.R


class HighScoreActivity : AppCompatActivity(), ScoreListFragment.OnScoreClickListener,
    OnMapReadyCallback {
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_score)

        val listFragment = supportFragmentManager.findFragmentById(R.id.high_score_FRAME_list) as? ScoreListFragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.high_score_FRAME_map) as? SupportMapFragment

        listFragment?.setOnScoreClickListener(this)
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        val defaultLocale = LatLng(35.3606, 138.7274) //defalt map to mt fuji
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocale, 8f))
    }

    override fun onScoreClicked(latitude: Double, longitude: Double) {
        if (googleMap != null) {
            val position = LatLng(latitude, longitude)

            googleMap?.clear() // clear old
            googleMap?.addMarker(MarkerOptions().position(position).title("Game Record Location"))
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f)) // Focus zoom
        } else {
            Toast.makeText(this, "Map interface is still loading...", Toast.LENGTH_SHORT).show()
        }
    }
}