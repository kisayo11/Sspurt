package com.kisayo.sspurt.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        }
}
