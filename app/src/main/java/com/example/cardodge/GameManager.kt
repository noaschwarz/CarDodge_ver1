package com.example.cardodge

import kotlin.random.Random

class GameManager(private val lifeCount: Int = 3) {

    var numberOfHits: Int = 0
        private set

    // represent our matrix
    val rows = 10
    val cols = 5
    val obstacleMatrix = Array(rows) { IntArray(cols) }

    var currentCarLane: Int = 2 // start game on middle lane (0=very Left, 1=Left, 2=Middle, 3=Right, 4=very Right)
    var score: Int = 0          // distance

    val isGameOver: Boolean
        get() = numberOfHits >= lifeCount

    fun shiftObstaclesDown(): Boolean {
        var hitDetected = false
        // check if obstacle in bottom row matches car lane -> we have a hit
        if (obstacleMatrix[rows - 1][currentCarLane]) {
            hitDetected = true
            numberOfHits++
        }
        // "overwrite" next row with the prev row
        for (r in rows - 1 downTo 1) {
            obstacleMatrix[r] = obstacleMatrix[r - 1].clone()
        }
        // generate new obstacle at top row at random
        obstacleMatrix[0] = BooleanArray(cols)
        if (Random.nextInt(100) < 50) {
            val targetSpawnLane = Random.nextInt(cols)
            obstacleMatrix[0][targetSpawnLane] = true
        }

        return hitDetected
    }

    //clean out crashed lemons
    fun clearCurrentCollisionObstacle() {
        obstacleMatrix[rows - 1][currentCarLane] = false
    }

    //move car lane left or right based on possible / action
    fun moveCarLeft() {
        if (currentCarLane > 0) currentCarLane--
    }

    fun moveCarRight() {
        if (currentCarLane < cols - 1) currentCarLane++
    }

    //reset locations of car when needed
    fun reset() {
        numberOfHits = 0
        currentCarLane = 1
        for (r in 0 until rows) {
            obstacleMatrix[r] = BooleanArray(cols)
        }
    }
}