package com.kisayo.sspurt.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.ExerciseRecord
import org.w3c.dom.Text

class ExerciseRecordAdapter(private val records: List<ExerciseRecord>) :
    RecyclerView.Adapter<ExerciseRecordAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val exerciseType: TextView = view.findViewById(R.id.exerciseType)
        val elapsedTime: TextView = view.findViewById(R.id.elapsedTime)
        val date: TextView = view.findViewById(R.id.date)
        val distance : TextView = view.findViewById(R.id.distance)
        val averageSpeed : TextView = view.findViewById(R.id.averageSpeed)
        val calories : TextView = view.findViewById(R.id.calories)
        val heartHealthScore : TextView = view.findViewById(R.id.heartHealthScore)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_record, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val record = records[position]
        holder.exerciseType.text = record.exerciseType
        holder.elapsedTime.text = "Elapsed Time: ${record.elapsedTime / 1000} seconds"
        holder.distance.text = "Distance: ${record.distance} km"
        holder.averageSpeed.text = "Average Speed: ${record.averageSpeed} km/h"
        holder.calories.text = "Calories: ${record.calories} kcal"
        holder.heartHealthScore.text = "Heart Health Score: ${record.heartHealthScore}"
        holder.date.text = "Date: ${record.date.toDate().toString().substring(0, 10)}" // 날짜 포맷 조정
    }

    override fun getItemCount() = records.size
}




