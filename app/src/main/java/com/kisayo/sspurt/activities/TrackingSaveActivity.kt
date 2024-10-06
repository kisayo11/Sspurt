package com.kisayo.sspurt.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityTrackingSaveBinding
import com.kisayo.sspurt.fragments.RecordDataFragment

class TrackingSaveActivity : AppCompatActivity() {

    val binding by lazy { ActivityTrackingSaveBinding.inflate(layoutInflater) }
    private lateinit var recordDataFragment: RecordDataFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Intent에서 `date`와 `ownerEmail` 수신
        val date = intent.getStringExtra("date")
        val ownerEmail = intent.getStringExtra("ownerEmail")

        // `DetailedRecordDataFragment` 인스턴스 생성 및 `Bundle`로 데이터 전달
        recordDataFragment = RecordDataFragment().apply {
            arguments = Bundle().apply {
                putString("date", date) // 전달받은 `date`
                putString("ownerEmail", ownerEmail) // 전달받은 `ownerEmail`
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.recordData_container, recordDataFragment)
            .commitNow()

        }
 }


