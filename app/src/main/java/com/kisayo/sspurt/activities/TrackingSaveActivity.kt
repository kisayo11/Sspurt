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

        recordDataFragment = RecordDataFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.recordData_container, recordDataFragment)
            .commitNow()

        }
 }


