package com.kisayo.sspurt.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityTrackingSaveBinding

class TrackingSaveActivity : AppCompatActivity() {

    val binding by lazy { ActivityTrackingSaveBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.uploadPictureBtn.setOnClickListener {  }

        }
    }
