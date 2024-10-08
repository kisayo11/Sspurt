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
        Log.d("RecordDataFragment", "onViewCreated called")

        userRepository = UserRepository(requireContext())
        barChart = binding.barChartAvgspeedpermin
        setupBarChartAvgspeedpermin()

        // Bundle에서 `exerciseRecordId` 수신
        val exerciseRecordId = arguments?.getString("exerciseRecordId")
        Log.d("RecordDataFragment", "Received exerciseRecordId: $exerciseRecordId")

        // `exerciseRecordId`가 항상 전달되므로 바로 데이터 조회
        if (exerciseRecordId != null) {
            fetchExerciseRecord(exerciseRecordId)
        } else {
            Log.e("RecordDataFragment", "No exerciseRecordId provided")
            // `exerciseRecordId`가 없는 상황에 대한 에러 처리 (발생하지 않는다면 이 부분은 필요 없음)
        }
        // save button
        binding.postBtn.setOnClickListener {
            val dialog = PostDialogFragment()
            val bundle = Bundle()
            bundle.putString("exerciseRecordId", exerciseRecordId)  // 전달할 exerciseRecordId
            dialog.arguments = bundle
            dialog.show(parentFragmentManager, "PostDialogFragment")
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

    private fun fetchExerciseRecord(exerciseRecordId: String) {
        Log.d("RecordDataFragment", "fetchExerciseRecord() called with ID: $exerciseRecordId") // 메서드 진입 확인

        db.collectionGroup("exerciseData")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("RecordDataFragment", "Documents found: ${documents.size()}") // 문서 수 출력

                var foundRecord: ExerciseRecord? = null // 매칭되는 레코드 저장

                documents.forEach { document ->
                    // 각 문서의 exerciseRecordId 필드 값 확인
                    val recordId = document.getString("exerciseRecordId")
                    Log.d("RecordDataFragment", "Document ID: ${document.id}, exerciseRecordId: $recordId")

                    // 원하는 exerciseRecordId와 비교
                    if (recordId == exerciseRecordId) { // 전달받은 ID 값과 비교
                        foundRecord = document.toObject(ExerciseRecord::class.java)
                    }
                }

                // 매칭되는 레코드가 있는 경우 UI 업데이트
                if (foundRecord != null) {
                    updateUIWithDetailedRecord(foundRecord!!) // UI 업데이트
                } else {
                    Log.d("RecordDataFragment", "No matching exerciseRecordId found.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error executing collectionGroup query: ", exception)
            }
//        db.collectionGroup("exerciseData")
//            .whereEqualTo("exerciseRecordId", exerciseRecordId)
//            .get()
//            .addOnSuccessListener { documents ->
//                Log.d("RecordDataFragment", "collectionGroup query executed successfully") // 쿼리 실행 확인
//
//                if (documents.isEmpty) {
//                    Log.d("RecordDataFragment", "No documents found for exerciseRecordId: $exerciseRecordId") // 쿼리 결과 없음
//                } else {
//                    Log.d("RecordDataFragment", "Documents found: ${documents.size()}") // 쿼리 결과 확인
//
//                    documents.forEach { document ->
//                        Log.d("RecordDataFragment", "Document ID: ${document.id}, Data: ${document.data}") // 가져온 문서 확인
//                    }
//
//                    val document = documents.first()
//                    val record = document.toObject(ExerciseRecord::class.java)
//                    Log.d("RecordDataFragment", "Fetched record: $record") // 변환된 데이터 확인
//                    updateUIWithDetailedRecord(record)
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firestore", "Error executing Firestore query: ", exception) // 쿼리 실패 확인
//            }
    }



    private fun updateUIWithDetailedRecord(record: ExerciseRecord) {
        Log.d("RecordDataFragment", "Updating UI with record data: $record")
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