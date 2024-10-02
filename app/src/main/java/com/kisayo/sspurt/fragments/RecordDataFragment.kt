package com.kisayo.sspurt.fragments

import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.text.format.DateUtils.formatElapsedTime
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.data.LatLngWrapper
import com.kisayo.sspurt.databinding.FragmentRecordDataBinding
import com.kisayo.sspurt.utils.UserRepository
import java.io.IOException
import java.util.Locale


class RecordDataFragment : Fragment() {
    private lateinit var binding: FragmentRecordDataBinding
    private lateinit var userRepository : UserRepository
    private lateinit var barChart : BarChart

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

        binding.postBtn.setOnClickListener {  }
        binding.deleteBtn.setOnClickListener {  }


        val email = userRepository.getCurrentUserEmail()
        fetchExerciseRecord(email)
    }
    private fun setupBarChartAvgspeedpermin(){
        barChart.description.text= ""

        // x축
        barChart.xAxis.apply{
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
        }
        // y축
        barChart.axisLeft.apply{
            setDrawGridLines(true)
        }
        barChart.axisRight.isEnabled = false

    }
    private fun formatElapsedTime(elapsedTime: Long): String {
        // 6초를 00:00:06으로 변환
        val hours = (elapsedTime / 3600) % 24
        val minutes = (elapsedTime / 60) % 60
        val seconds = elapsedTime % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    private fun fetchExerciseRecord(email : String?){
        val db = FirebaseFirestore.getInstance()
        db.collection("account")
            .document(email!!)
            .collection("exerciseData")
            .orderBy("date",Query.Direction.DESCENDING) //생성일 기준으로 내림차순
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first() // 첫 번째 문서 가져오기
                    val record = document.toObject(ExerciseRecord::class.java)

                    val drawableResId = when(record.exerciseType){
                        "running" -> R.drawable.icon_running100
                        "cycling" -> R.drawable.icon_ridding100
                        "hiking" -> R.drawable.icon_treckking100
                        "trailrunning" -> R.drawable.icon_trail100
                        else -> R.drawable.logo_sspurt
                    }
                    binding.ExersiceTypeIv.setImageResource(drawableResId)

                    //운동시간 포맷팅
                    binding.ExersiceTimeTv.text = formatElapsedTime(record.elapsedTime)

                    //이동거리
                    val distanceInKm = record.distance / 1000.0 // m를 km으로 변환
                    binding.ExersiceDistanceTv.text = String.format("%.2f km", distanceInKm)

                    //평균속도 (pace) 포맷팅
                    binding.ExersicePaceTv.text = if(record.averageSpeed == 0.0) "0" else formatSpeedToString(record.averageSpeed)

                    //최고속도 (maxspeed) 포맷팅
                    binding.ExersiceMaxspeedTv.text = if(record.maxSpeed == 0.0) "0" else formatSpeedToString(record.maxSpeed)

                    // 심장 강화 점수
                    binding.ExersiceHeartScoreTv.text = record.heartHealthScore.toString()

                    // 칼로리 소모량
                    binding.ExersiceCalorieTv.text = String.format("%.2f", record.calories)

                    // 차트에 평균 속도 추가
                    updateBarChart(record.averageSpeed)

                    val location = record.currentLocation
                    if (location != null) {
                        getAddressFromLatLng(location)
                    }
                }
            }
            .addOnFailureListener { exception ->
                // 오류 발생 시 처리 (현재는 무시)
            }
    }

    private fun getAddressFromLatLng(location: LatLngWrapper) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val latLng = LatLng(location.latitude, location.longitude)

        Thread {
            try {
                // 지오코딩을 통해 주소 정보를 가져옴
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses != null) {
                    if (addresses.isNotEmpty()) {
                        // 가져온 주소에서 "동"까지만 추출
                        val address = addresses[0]?.locality // 시/구 정보
                        val subLocality = addresses[0]?.subLocality // 동 정보

                        // null 체크
                        if (address != null && subLocality != null) {
                            val fullAddress = "$address $subLocality"

                            activity?.runOnUiThread {
                                binding.addressTv.text = fullAddress // 주소를 TextView에 출력
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                // 오류 처리 없음
            }
        }.start()
    }

    private fun formatSpeedToString(speedKmh: Double): String {
        // km/h를 mm'ss" 형식으로 변환
        val speedInMetersPerMinute = (speedKmh * 1000) / 60 // km/h를 m/min으로 변환
        val totalSeconds = (60 / speedInMetersPerMinute).toInt() // m/min을 초로 변환
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%d'%02d\"", minutes, seconds)
    }

    private fun updateBarChart(averageSpeed: Double) {
        // 평균 속도를 바 차트에 추가
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, averageSpeed.toFloat())) // 예시로 첫 번째 바에 추가

        val dataSet = BarDataSet(entries, "평균 속도 (분/초)").apply {
            color = Color.BLUE
            valueTextColor = Color.WHITE
            valueTextSize = 16f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f
        barChart.data = barData
        barChart.invalidate() // 차트 업데이트
    }


}