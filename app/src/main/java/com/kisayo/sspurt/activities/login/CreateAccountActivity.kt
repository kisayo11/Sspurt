package com.kisayo.sspurt.activities.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.databinding.ActivityCreateAcountBinding

class CreateAccountActivity : AppCompatActivity() {

    val binding by lazy { ActivityCreateAcountBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



    }
}