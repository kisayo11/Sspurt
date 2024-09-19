package com.kisayo.sspurt.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityTrackingStartBinding

class TrackingStartActivity : AppCompatActivity() {

    val binding by lazy { ActivityTrackingStartBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.ivPick.setOnClickListener { startActivity(Intent(this,GpsConfirmActivity::class.java))
            finish()
        }

        }
    }
