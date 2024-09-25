package com.kisayo.sspurt.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kisayo.sspurt.activities.TrackingStartActivity
import com.kisayo.sspurt.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }

    lateinit var binding: FragmentHomeBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // 플로팅버튼 "Start" 클릭리스너
        binding.startFab.setOnClickListener {
            val intent = Intent(requireContext(), TrackingStartActivity::class.java)
            startActivity(intent)
        }
    }
}
