package com.kisayo.sspurt.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.databinding.ActivityTrackingSaveBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.kisayo.sspurt.adapter.ExerciseRecordAdapter

class TrackingSaveActivity : AppCompatActivity() {

    val binding by lazy { ActivityTrackingSaveBinding.inflate(layoutInflater) }
    private lateinit var adapter: ExerciseRecordAdapter
    private val exerciseRecords = mutableListOf<ExerciseRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

//        binding.uploadPictureBtn.setOnClickListener {  }

        // RecyclerView 설정
        adapter = ExerciseRecordAdapter(exerciseRecords)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        fetchExerciseRecords() // 데이터 가져오기
    }
    private fun fetchExerciseRecords() {
        val db = FirebaseFirestore.getInstance()
        val userEmail = getCurrentUserEmail() // 로그인된 이메일 가져오기

        // Firestore에서 운동 기록을 가져옴
        db.collection("account")
            .document(userEmail)
            .collection("exerciseData")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val record = document.toObject(ExerciseRecord::class.java)
                    exerciseRecords.add(record)
                }
                updateChart() // 그래프 업데이트
                adapter.notifyDataSetChanged() // RecyclerView 데이터 변경 알림
            }
            .addOnFailureListener { exception ->
                // 에러 처리
                Log.w("MainActivity", "Error getting documents: ", exception)
            }
    }

    private fun getCurrentUserEmail(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.email ?: "" // 사용자가 로그인하지 않은 경우 빈 문자열 반환
    }

    private fun updateChart() {
        val speedEntries = mutableListOf<Entry>()
        val paceEntries = mutableListOf<Entry>()

        // 10분마다의 데이터 집계
        val groupedRecords = exerciseRecords.groupBy {
            it.elapsedTime / (10 * 60 * 1000) // 10분 단위로 그룹화
        }

        groupedRecords.forEach { (key, records) ->
            val averageSpeed = records.map { it.averageSpeed }.average().toFloat()
            val totalDistance = records.map { it.distance }.sum().toFloat()

            // 킬로미터당 페이스 (시간당 거리로 계산)
            val totalTimeInMinutes = records.map { it.elapsedTime }.sum() / 60000.0f // 총 소요 시간 (분)
            val pace = if (totalDistance > 0) totalTimeInMinutes / totalDistance else 0f // 킬로미터당 분

            speedEntries.add(Entry(key.toFloat(), averageSpeed))
            paceEntries.add(Entry(key.toFloat(), pace))
        }

        // 평균 속도 그래프
        val speedDataSet = LineDataSet(speedEntries, "Average Speed (km/h)")
        speedDataSet.color = Color.BLUE
        speedDataSet.valueTextColor = Color.BLACK
        speedDataSet.lineWidth = 2f
        speedDataSet.setDrawCircles(true)
        speedDataSet.setCircleColor(Color.RED)

        // 페이스 그래프
        val paceDataSet = LineDataSet(paceEntries, "Pace (min/km)")
        paceDataSet.color = Color.GREEN
        paceDataSet.valueTextColor = Color.BLACK
        paceDataSet.lineWidth = 2f
        paceDataSet.setDrawCircles(true)
        paceDataSet.setCircleColor(Color.YELLOW)

        // 두 데이터셋을 결합하여 그래프에 표시
        val lineData = LineData(speedDataSet, paceDataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate() // 그래프 업데이트
    }
}

