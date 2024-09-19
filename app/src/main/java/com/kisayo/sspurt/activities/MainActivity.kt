package com.kisayo.sspurt.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.activities.login.CreateAcountActivity
import com.kisayo.sspurt.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        // 플로팅버튼 "Start" 클릭리스너
        binding.startFab.setOnClickListener {
            startActivity(Intent(this,TrackingStartActivity::class.java))
        }


        }
}
