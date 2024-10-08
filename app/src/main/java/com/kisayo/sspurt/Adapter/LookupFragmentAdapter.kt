package com.kisayo.sspurt.Adapter

import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.CombinedRecord
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LookupFragmentAdapter(
    private val context: Context,
    private val combinedRecords: List<CombinedRecord>,
    private val onItemClick: (String) -> Unit // 클릭 리스너
) : RecyclerView.Adapter<LookupFragmentAdapter.ExerciseRecordViewHolder>() {

    inner class ExerciseRecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nicknameTextView: TextView = itemView.findViewById(R.id.nickname_tv)
        private val exercisetypeView: ImageView = itemView.findViewById(R.id.exerciseType_cv)
        private val distanceTextView: TextView =
            itemView.findViewById(R.id.ExerciseDistance_tv_item)

        //private val averageSpeedTextView: TextView = itemView.findViewById(R.id.ExerciseSpeed_tv_item)
        private val elapsedTimeTextView: TextView = itemView.findViewById(R.id.ExerciseTime_tv_item)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTag)
        private val dateTextView: TextView = itemView.findViewById(R.id.ExerciseDate_tv_item)
        private val photoImageView: ImageView = itemView.findViewById(R.id.picture_iv)
        private val profileImageView: ImageView = itemView.findViewById(R.id.profile_cv)

        fun bind(combinedRecord: CombinedRecord) {
            val exerciseRecord = combinedRecord.exerciseRecord
            val userAccount = combinedRecord.userAccount


            nicknameTextView.text = userAccount.username
            elapsedTimeTextView.text = formatElapsedTime(exerciseRecord.elapsedTime)
            distanceTextView.text =
                String.format("%.2f km", exerciseRecord.distance / 1000) // 이동 거리
            //averageSpeedTextView.text = String.format("%.2f km/h", exerciseRecord.averageSpeed) // 평균 속도
            //averageSpeedTextView.text = formatSpeedToCoordinates(exerciseRecord.averageSpeed) // 평균 속도


            // 위치 정보를 주소로 변환
            val location = exerciseRecord.currentLocation
            val address = location?.let { getAddress(it.latitude, it.longitude) }
            locationTextView.text = address ?: "" // 위치 표시

            val formattedDate = formatDate(exerciseRecord.date.toDate())
            dateTextView.text = formattedDate // 날짜 표시


            // 프로필 URL을 가져와서 이미지 뷰에 로드하는 로직 추가 (Glide 또는 Picasso 사용)
            userAccount.profileImageUrl.let { url ->
                Glide.with(itemView.context).load(url).into(profileImageView)
            }

            // 사진 URL을 가져와서 이미지 뷰에 로드하는 로직 추가 (Glide 또는 Picasso 사용)
            exerciseRecord.photoUrl?.let { url ->
                Glide.with(itemView.context).load(url).into(photoImageView)
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

            itemView.setOnClickListener {
                onItemClick(exerciseRecord.exerciseRecordId) // 클릭 시 recordId 전달

            }
        }

        // 날짜 포맷팅 함수
        private fun formatDate(date: Date): String {
            val dateFormat = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
            return dateFormat.format(date)
        }

        private fun getAddress(latitude: Double, longitude: Double): String {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addressLine: String
            return try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addressLine = addresses?.get(0)?.getAddressLine(0) ?: "" // 첫 번째 주소 라인
                val addressComponents = addressLine.split(" ")
                // 성수동 정보를 반환
                "${addressComponents[3]}"
            } catch (e: IOException) {
                e.printStackTrace()
                "주소를 가져올 수 없습니다." // 예외 발생 시 기본 메시지 반환
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseRecordViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_list_lookupfragment, parent, false)
        return ExerciseRecordViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExerciseRecordViewHolder, position: Int) {
        val combinedRecord = combinedRecords[position]
        holder.bind(combinedRecord)


//        val record = combinedRecords[position]
//
////         구버전 롤백용
//        val combinedRecord = combinedRecords[position]
//        holder.bind(combinedRecord)

//        holder.itemView.setOnClickListener {
//            val intent = Intent(context, TrackingSaveActivity::class.java)
//            intent.putExtra("ownerEmail", record.exerciseRecord.ownerEmail)
//            intent.putExtra("date", record.exerciseRecord.date)
//
//            context.startActivity(intent)
//        }


    }

    override fun getItemCount(): Int {
        return combinedRecords.size // exerciseRecords의 크기를 반환
    }

    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = (elapsedTime / 3600000).toInt()
        val minutes = (elapsedTime % 3600000 / 60000).toInt()
        val seconds = (elapsedTime % 60000 / 1000).toInt()
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