package com.kisayo.sspurt.Adapter

import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.ExerciseRecord
import java.io.IOException
import java.util.*
import java.text.SimpleDateFormat



class HomeFragmentAdapter(
    private val context: Context,
    private val exerciseRecords: List<ExerciseRecord>,
    private val onItemClick: (String) -> Unit // 클릭리스너
) : RecyclerView.Adapter<HomeFragmentAdapter.ExerciseRecordViewHolder>() {

    inner class ExerciseRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exercisetypeView: ImageView = itemView.findViewById(R.id.exerciseType_iv_item)
        private val distanceTextView: TextView = itemView.findViewById(R.id.ExersiceDistance_tv_item)
        private val averageSpeedTextView: TextView = itemView.findViewById(R.id.ExersiceSpeed_tv_item)
        private val elapsedTimeTextView: TextView = itemView.findViewById(R.id.ExersiceTime_tv_item)
        private val locationTextView: TextView = itemView.findViewById(R.id.ExersiceLocation_tv_item)
        private val dateTextView: TextView = itemView.findViewById(R.id.ExersiceDate_tv_item)
        private val photoImageView: ImageView = itemView.findViewById(R.id.exersicePic_iv_item)

        fun bind(exerciseRecord: ExerciseRecord) {
//            exerciseTypeTextView.text = exerciseRecord.exerciseType // 운동 종류
            distanceTextView.text = String.format("%.2f km", exerciseRecord.distance / 1000) // 이동 거리
//            averageSpeedTextView.text = String.format("%.2f km/h", exerciseRecord.averageSpeed) // 평균 속도
            averageSpeedTextView.text = formatSpeedToCoordinates(exerciseRecord.averageSpeed / 1000) // 평균 속도
            elapsedTimeTextView.text = formatElapsedTime(exerciseRecord.elapsedTime)

            // 위치 정보를 주소로 변환
            val location = exerciseRecord.currentLocation
            val address = location?.let { getAddress(it.latitude, it.longitude) }
            locationTextView.text = address ?: "" // 위치 표시

            val formattedDate =formatDate(exerciseRecord.date.toDate())
            dateTextView.text = formattedDate // 날짜 표시

            // 사진 URL을 가져와서 이미지 뷰에 로드하는 로직 추가 (Glide 또는 Picasso 사용)
            exerciseRecord.photoUrl?.let { url ->
                Glide.with(itemView.context)
                    .load(url)
                    .into(photoImageView)
            }

            // 아이템 클릭 시 exerciseRecordId 전달
            itemView.setOnClickListener {
                onItemClick(exerciseRecord.exerciseRecordId) // exerciseRecordId 전달
            }


            exercisetypeView.setImageResource(
                when (exerciseRecord.exerciseType) {
                    "running" -> R.drawable.icon_running100 // 달리기 아이콘
                    "cycling" -> R.drawable.icon_ridding100 // 사이클링 아이콘
                    "hiking" -> R.drawable.icon_treckking100 // 하이킹 아이콘
                    "trailrunning" -> R.drawable.icon_trail100 // 트레일러닝 아이콘
                    else -> R.drawable.logo_sspurt // 기본 아이콘
                }
            )

        }
        // 날짜 포맷팅 함수
        private fun formatDate(date: Date): String {
            val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
            return dateFormat.format(date)
        }

        private fun getAddress(latitude: Double, longitude: Double): String {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addressLine: String
            return try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addressLine = addresses?.get(0)?.getAddressLine(0) ?: "" // 첫 번째 주소 라인
                val addressComponents = addressLine.split(" ")

                // 서울특별시, 성동구, 성수동 정보를 반환
                "${addressComponents[1]} ${addressComponents[2]} ${addressComponents[3]}"

            } catch (e: IOException) {
                e.printStackTrace()
                "주소를 가져올 수 없습니다." // 예외 발생 시 기본 메시지 반환
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseRecordViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_list_homefragment, parent, false)
        return ExerciseRecordViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return exerciseRecords.size
    }

    override fun onBindViewHolder(holder: ExerciseRecordViewHolder, position: Int) {
        val exerciseRecord = exerciseRecords[position]
        holder.bind(exerciseRecord)
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = (elapsedTime / 3600).toInt() // 시
        val minutes = ((elapsedTime % 3600) / 60).toInt() // 분
        val seconds = (elapsedTime % 60).toInt() // 초
        return String.format("%02d:%02d:%02d", hours, minutes, seconds) // HH:mm:ss 형식으로 포맷
    }

    // 평균 속도를 "00'00''" 형식으로 변환하는 함수
    private fun formatSpeedToCoordinates(speed: Double): String {
        // km/h의 정수부와 소수부를 분리
        val km = speed.toInt() // km/h의 정수 부분
        val meters = ((speed - km) * 100).toInt() // km/h를 m 단위로 변환한 후 소수부를 100으로 변환
        return String.format("%02d'%02d''", km, meters)
    }

}