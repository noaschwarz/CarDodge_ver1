package com.example.cardodge.utilities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.cardodge.R

class ScoreListFragment : Fragment(){
    // Interface to notify the hosting activity of coordinate changes
    interface OnScoreClickListener {
        fun onScoreClicked(latitude: Double, longitude: Double)
    }

    private var scoreClickListener: OnScoreClickListener? = null

    fun setOnScoreClickListener(listener: OnScoreClickListener) {
        this.scoreClickListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_score_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.list_RCL_scores)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Fetch high scores out of our database manager
        val scoreManager = ScoreManager(requireContext())
        val recordsList = scoreManager.getHighScores()

        // Bind adapter to interface callback
        recyclerView.adapter = ScoreAdapter(recordsList) { clickedRecord ->
            scoreClickListener?.onScoreClicked(clickedRecord.latitude, clickedRecord.longitude)
        }

        return view
    }

    private class ScoreAdapter(
        private val items: List<ScoreRecord>,
        private val onItemClick: (ScoreRecord) -> Unit
    ) : RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
            return ScoreViewHolder(v)
        }

        override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, position + 1, onItemClick)
        }

        override fun getItemCount(): Int = items.size

        class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val rankText = itemView.findViewById<TextView>(R.id.item_lbl_rank)
            val nameText = itemView.findViewById<TextView>(R.id.item_lbl_name)
            val dateText = itemView.findViewById<TextView>(R.id.item_lbl_date)
            val scoreText = itemView.findViewById<TextView>(R.id.item_lbl_score)

            fun bind(record: ScoreRecord, position: Int, clickAction: (ScoreRecord) -> Unit) {
                rankText.text = "#$position"
                nameText.text = record.playerName
                scoreText.text = String.format("%05d m", record.score)

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateText.text = sdf.format(Date(record.timestamp))

                itemView.setOnClickListener { clickAction(record) }
            }
        }
    }
}