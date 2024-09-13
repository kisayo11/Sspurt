package com.kisayo.sspurt.activities.login

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityCreateAcountBinding

class CreateAcountActivity : AppCompatActivity() {

    val binding by lazy { ActivityCreateAcountBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



    }
}