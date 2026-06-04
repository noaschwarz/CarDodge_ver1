package com.example.cardodge.utilities

import kotlin.random.Random

class GameManager(private val lifeCount: Int = 3) {


    // represent our matrix
    val rows = 6
    val cols = 5
    // matrix states 0 = Empty, 1 = Lemon, 2 = Coin
    val obstacleMatrix = Array(rows) { IntArray(cols) }

    var currentCarLane: Int = 2 // start game on middle lane (0=very Left, 1=Left, 2=Middle, 3=Right, 4=very Right)
    var score: Int = 0          // distance
    var numberOfHits: Int = 0
    var distanceOdometer: Int = 0
    var coinScore: Int = 0

    val isGameOver: Boolean
        get() = numberOfHits >= lifeCount

    fun shiftObstaclesDown(): Int {
        if (!isGameOver) { //add to distance if we keep going
            distanceOdometer += 10
        }

        val collisionType = checkCollision() // check if we have a hit
        if (collisionType != 0) { // if we are hit, clear it out
            obstacleMatrix[rows - 1][currentCarLane] = 0
        }

        for (r in (rows - 1) downTo 1) { // move items down
            for (c in 0 until cols) {
                obstacleMatrix[r][c] = obstacleMatrix[r - 1][c]
            }
        }

        generateNewRowItems() //spawn new items (lemon / coin)
        return collisionType // return what type of hit we had 0=no 1=lemon 2=coin
    }

    private fun generateNewRowItems() {
        for (c in 0 until cols) { //make sure first row is clear
            obstacleMatrix[0][c] = 0
        }
        val spawnLane = Random.Default.nextInt(cols) //get random line and drop rate
        val spawnChance = Random.Default.nextInt(100)

        when {
            spawnChance < 60 -> { // 60% for a leon
                obstacleMatrix[0][spawnLane] = 1
            }
            spawnChance in 60..79 -> { //20% for a coin
                obstacleMatrix[0][spawnLane] = 2
            }
        }
    }

    //clean out items we hit
    fun clearCurrentCollisionObstacle() {
        obstacleMatrix[rows - 1][currentCarLane] = 0
    }

    private fun checkCollision(): Int {
        val bottomItem = obstacleMatrix[rows - 1][currentCarLane] // see what item we have
        return when (bottomItem) {
            1 -> {
                numberOfHits++
                1 // lemon
            }
            2 -> {
                coinScore += 5 // get 5 points
                2 // coin
            }
            else -> 0
        }
    }

    //move car lane left or right based on possible / action
    fun moveCarLeft() {
        if (currentCarLane > 0) currentCarLane--
    }

    fun moveCarRight() {
        if (currentCarLane < cols - 1) currentCarLane++
    }

    //reset locations of car, all items, and set all info at int
    fun reset() {
        numberOfHits = 0
        currentCarLane = 2
        distanceOdometer = 0
        coinScore = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                obstacleMatrix[r][c] = 0
            }
        }
    }
}