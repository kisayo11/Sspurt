package com.kisayo.sspurt.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityTrackingSaveBinding
import com.kisayo.sspurt.fragments.RecordDataFragment
import java.util.Date

class TrackingSaveActivity : AppCompatActivity() {

    private val binding by lazy { ActivityTrackingSaveBinding.inflate(layoutInflater) }
    private lateinit var recordDataFragment: RecordDataFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Intent에서 `date`와 `ownerEmail` 수신
        val dateString = intent.getStringExtra("date")
        val ownerEmail = intent.getStringExtra("ownerEmail")
        Log.d("TrackingSaveActivity", "Received Email: $ownerEmail, Date: $dateString")

        if (ownerEmail == null) {
            Log.e("TrackingSaveActivity", "Owner email is null")
        }
        if (dateString == null) {
            Log.e("TrackingSaveActivity", "Date string is null")
        }

        // 데이터 유효성 검사
        if (ownerEmail != null && dateString != null) {
            // `RecordDataFragment` 인스턴스 생성 및 `Bundle`로 데이터 전달
            recordDataFragment = RecordDataFragment().apply {
                arguments = Bundle().apply {
                    putString("date", dateString) // Date를 String 형식으로 전달
                    putString("ownerEmail", ownerEmail) // 전달받은 `ownerEmail`
                }
            }

            // Fragment 전환
            supportFragmentManager.beginTransaction()
                .replace(R.id.recordData_container, recordDataFragment)
                .commitNow()
        } else {
            // 데이터가 유효하지 않은 경우 처리
            Toast.makeText(this, "Invalid data received", Toast.LENGTH_SHORT).show()
            finish() // Activity 종료
        }
    }
}