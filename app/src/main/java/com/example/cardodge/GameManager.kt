package com.example.cardodge

class GameManager(private val lifeCount: Int = 3) {

    var numberOfHits: Int = 0
        private set

    val isGameOver: Boolean
        get() = numberOfHits >= lifeCount

    fun registerHit() {
        numberOfHits++
    }

    fun reset() {
        numberOfHits = 0
    }
}