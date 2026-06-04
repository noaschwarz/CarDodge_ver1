package com.example.cardodge.utilities

data class ScoreRecord(
    val playerName: String,
    val score: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double
)