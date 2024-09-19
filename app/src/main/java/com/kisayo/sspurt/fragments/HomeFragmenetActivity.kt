package com.kisayo.sspurt.fragments

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityHomeFragmenetBinding
import com.kisayo.sspurt.databinding.ActivityTrackingStartBinding

class HomeFragmenetActivity : AppCompatActivity() {

    val binding by lazy { ActivityHomeFragmenetBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        }
    }
