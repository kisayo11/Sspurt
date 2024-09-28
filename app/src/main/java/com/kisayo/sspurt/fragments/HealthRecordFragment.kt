package com.kisayo.sspurt.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kisayo.sspurt.databinding.FragmentHealthRecordBinding


class HealthRecordFragment : Fragment() {

    private lateinit var binding: FragmentHealthRecordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHealthRecordBinding.inflate(inflater, container, false)
        return binding.root
    }//onCreate...

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recordAni.setOnClickListener {
            binding.countdownAni.visibility = View.VISIBLE
            binding.countdownAni.playAnimation()

            // 애니메이션 끝나면 GONE으로 설정
            binding.countdownAni.addAnimatorListener(object : AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator) {
                    binding.countdownAni.visibility = View.GONE
                    binding.recordAni.visibility= View.GONE

                    binding.pauseIb.visibility = View.VISIBLE

                }
            })
        }

        binding.pauseIb.setOnClickListener {
            if(binding.stopIb.visibility == View.VISIBLE){
                binding.stopIb.visibility = View.INVISIBLE
            } else{
                binding.stopIb.visibility = View.VISIBLE
            }
        }
    }
}


