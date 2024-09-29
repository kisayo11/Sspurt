package com.kisayo.sspurt.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.TrackingSaveActivity
import com.kisayo.sspurt.activities.TrackingStartActivity
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


        // 레코드 버튼(animationview) 클릭 리스너
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

        //pause 버튼 클릭 리스너
        binding.pauseIb.setOnClickListener {
            if(binding.stopIb.visibility == View.VISIBLE){
                binding.stopIb.visibility = View.INVISIBLE
            } else{
                binding.stopIb.visibility = View.VISIBLE
            }
        }

        //stop 버튼 클릭 리스너
        // 초기 색상 설정 (검은색 아이콘)
        binding.stopIb.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        binding.stopIb.setOnLongClickListener {
            // 애니메이션을 위한 ValueAnimator 설정
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 500 // 애니메이션 시간

            animator.addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedValue as Float
                val color = blendColors(Color.BLACK, Color.BLUE, fraction) // 색상 혼합
                binding.stopIb.setColorFilter(color, PorterDuff.Mode.SRC_IN) // 아이콘 색상 업데이트
            }

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션이 끝난 후 액티비티로 이동
                    val intent = Intent(requireContext(), TrackingSaveActivity::class.java)
                    startActivity(intent)
                }
            })

            animator.start()
            true // 이벤트 소비
        }
    }

    // 색상 혼합 함수
    private fun blendColors(color1: Int, color2: Int, fraction: Float): Int {
        val alpha = (Color.alpha(color1) + fraction * (Color.alpha(color2) - Color.alpha(color1))).toInt()
        val red = (Color.red(color1) + fraction * (Color.red(color2) - Color.red(color1))).toInt()
        val green = (Color.green(color1) + fraction * (Color.green(color2) - Color.green(color1))).toInt()
        val blue = (Color.blue(color1) + fraction * (Color.blue(color2) - Color.blue(color1))).toInt()
        return Color.argb(alpha, red, green, blue)
    }
}


