package com.kisayo.sspurt.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kisayo.sspurt.databinding.FragmentMyAccountBinding


class MyAccountFragment : Fragment(){
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentMyAccountBinding.inflate(inflater,container,false)
        return binding.root
    }
    lateinit var binding:FragmentMyAccountBinding

}