package com.kisayo.sspurt.fragments

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityHomeFragmenetBinding
import com.kisayo.sspurt.databinding.ActivityLookUpFragmentBinding

class LookUpFragmentActivity : AppCompatActivity() {

    val binding by lazy { ActivityLookUpFragmentBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

    }
}