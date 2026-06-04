package com.example.cardodge.utilities

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScoreManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("HighScoresPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getHighScores(): List<ScoreRecord> { // get top 10
        val json = sharedPreferences.getString("high_scores_list", null) ?: return emptyList()
        val type = object : TypeToken<List<ScoreRecord>>() {}.type
        return gson.fromJson<List<ScoreRecord>>(json, type).sortedByDescending { it.score }
    }

    // add new record if within the top 10
    fun addScore(record: ScoreRecord) {
        val currentScores = getHighScores().toMutableList()
        currentScores.add(record)
        val topTen = currentScores.sortedByDescending { it.score }.take(10) // sort and keep top 10
        sharedPreferences.edit().putString("high_scores_list", gson.toJson(topTen)).apply()
    }
}