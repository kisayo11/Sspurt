package com.kisayo.sspurt.activities.preference

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    val binding by lazy { ActivityProfileBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

    }
}