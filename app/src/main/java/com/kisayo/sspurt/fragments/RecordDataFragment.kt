package com.kisayo.sspurt.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kisayo.sspurt.Helpers.FirestoreHelper
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.databinding.FragmentRecordDataBinding
import com.kisayo.sspurt.utils.UserRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class RecordDataFragment : Fragment() {
    private lateinit var binding: FragmentRecordDataBinding
    private lateinit var userRepository: UserRepository
    private lateinit var barChart: BarChart
    private val db = FirebaseFirestore.getInstance() // Firestore 인스턴스 초기화

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository = UserRepository(requireContext())
        barChart = binding.barChartAvgspeedpermin
        setupBarChartAvgspeedpermin()

        // Bundle에서 `ownerEmail`과 `date` 수신
        val ownerEmail = arguments?.getString("ownerEmail")
        val dateString = arguments?.getString("date") // String 형식으로 수신

        // String을 Timestamp로 변환
        val date = dateString?.let { stringToTimestamp(it) }

        // 전달된 데이터에 따라 다른 로직 처리
        if (ownerEmail != null && date != null) {
            fetchExerciseRecord(ownerEmail, date) // 이메일과 날짜를 사용하여 데이터 조회
        } else {
            // 본인의 기록을 가져오는 경우 (기본 동작)
            val email = userRepository.getCurrentUserEmail()
            fetchRecentExerciseRecord(email) // 최근 운동 기록 불러오기
        }

        // save button
        binding.postBtn.setOnClickListener {
            val postDialog = PostDialogFragment()
            postDialog.show(parentFragmentManager, "postDialogFragment")
        }

        // delete button
        binding.deleteBtn.setOnClickListener {
            val email = userRepository.getCurrentUserEmail() // 사용자 이메일 가져오기
            deleteRecentExerciseRecord(email!!)
            binding.deleteBtn.visibility = View.INVISIBLE

            // 현재 액티비티 종료
            requireActivity().finish()

            // 모든 액티비티 종료 후 메인 액티비티로 이동
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun stringToTimestamp(dateString: String): Timestamp? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = format.parse(dateString)
            Timestamp(date!!)
        } catch (e: Exception) {
            null // 변환 실패 시 null 반환
        }
    }

    private fun fetchExerciseRecord(ownerEmail: String, date: Timestamp) {
        db.collection("account").document(ownerEmail).collection("exerciseData")
            .whereEqualTo("date", date) // 정확한 날짜에 해당하는 기록 조회
            .get()
            .addOnSuccessListener { documents ->
                Log.d("RecordDataFragment", "Documents found: ${documents.size()}")
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val record = document.toObject(ExerciseRecord::class.java)
                    updateUIWithDetailedRecord(record)
                } else {
                    Log.d("RecordDataFragment", "No documents found for the given date.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }
    }
    private fun updateUIWithDetailedRecord(record: ExerciseRecord) {
        binding.ExersiceTimeTv.text = formatElapsedTime(record.elapsedTime)
        val distanceInKm = record.distance / 1000.0
        binding.ExersiceDistanceTv.text = String.format("%.2f km", distanceInKm)
        val formattedDate = formatDate(record.date.toDate())
        binding.ExersiceDateTv.text = formattedDate
        binding.ExersicePaceTv.text = if (record.averageSpeed == 0.0) "0" else formatSpeedToString(record.averageSpeed)
        binding.ExersiceMaxspeedTv.text = if (record.maxSpeed == 0.0) "0" else formatSpeedToString(record.maxSpeed)
        binding.ExersiceHeartScoreTv.text = record.heartHealthScore.toString()
        binding.ExersiceCalorieTv.text = String.format("%.2f", record.calories)

        // 운동 타입 표시
        binding.ExersiceTypeIv.setImageResource(
            when (record.exerciseType) {
                "running" -> R.drawable.icon_running100
                "cycling" -> R.drawable.icon_ridding100
                "hiking" -> R.drawable.icon_treckking100
                "trailrunning" -> R.drawable.icon_trail100
                else -> R.drawable.logo_sspurt
            }
        )

        updateBarChart(record.averageSpeed)
    }

    private fun deleteRecentExerciseRecord(email: String) {
        // FirestoreHelper 인스턴스 생성 및 삭제 로직
        val firestoreHelper = FirestoreHelper()
        firestoreHelper.deleteRecentExerciseRecord(email, onSuccess = {
            Toast.makeText(requireContext(), "최근 운동 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        }, onFailure = { e ->
            Toast.makeText(requireContext(), "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        })
    }

    private fun setupBarChartAvgspeedpermin() {
        barChart.description.text = ""
        // x축
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }
        // y축
        barChart.axisLeft.apply {
            setDrawGridLines(true)
        }
        barChart.axisRight.isEnabled = false
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        // 초를 00:00:00으로 변환
        val hours = (elapsedTime / 3600) % 24
        val minutes = (elapsedTime / 60) % 60
        val seconds = elapsedTime % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun fetchRecentExerciseRecord(email: String?) {
        db.collection("account").document(email!!).collection("exerciseData")
            .orderBy("date", Query.Direction.DESCENDING) // 생성일 기준으로 내림차순
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first() // 첫 번째 문서 가져오기
                    val record = document.toObject(ExerciseRecord::class.java)

                    // UI 업데이트 로직
                    updateUIWithDetailedRecord(record)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ", exception)
            }
    }

    // 날짜 포맷팅 함수
    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        return dateFormat.format(date)
    }

    private fun formatSpeedToString(speedKmh: Double): String {
        val speedInMetersPerMinute = (speedKmh * 1000) / 60
        val totalSeconds = (60 / speedInMetersPerMinute).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%d'%02d\"", minutes, seconds)
    }
    private fun updateBarChart(averageSpeed: Double) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, averageSpeed.toFloat())) // 예시로 첫 번째 바에 추가

        val dataSet = BarDataSet(entries, "평균 속도 (분/초)").apply {
            color = Color.BLUE
            valueTextColor = Color.WHITE
            valueTextSize = 16f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f
        binding.barChartAvgspeedpermin.data = barData
        binding.barChartAvgspeedpermin.invalidate() // 차트 업데이트
    }
}