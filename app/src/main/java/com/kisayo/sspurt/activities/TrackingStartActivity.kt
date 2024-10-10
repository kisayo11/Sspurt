package com.kisayo.sspurt.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityTrackingStartBinding

class TrackingStartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackingStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackingStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱 시작 시 최근 선택한 아이콘 불러오기
        loadIcon()

        animateIconIndefinitely()

        // 버튼 클릭 시 아이콘 설정 및 배경색 변경
        binding.ibRunning.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_running100)
            saveToPreferences("running")
            resetBackgrounds()
            binding.ibRunning.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
        }

        binding.ibHiking.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_treckking100)
            saveToPreferences("hiking")
            resetBackgrounds()
            binding.ibHiking.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
        }

        binding.ibTrailrunning.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_trail100)
            saveToPreferences("trailrunning")
            resetBackgrounds()
            binding.ibTrailrunning.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
        }

        binding.ibCycling.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_ridding100)
            saveToPreferences("cycling")
            resetBackgrounds()
            binding.ibCycling.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
        }

        // ivPick 클릭 시 SharedPreferences에 저장된 값 사용
        binding.ivPick.setOnClickListener {
            saveToPreferences(loadFromPreferences() ?: "default_value")
            startActivity(Intent(this,GpsConfirmActivity::class.java))
            finish()
        }
    }

    // 애니메이션을 반복해서 실행하는 메서드
    private fun animateIconIndefinitely() {
        val scaleX = ObjectAnimator.ofFloat(binding.ivPick, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivPick, "scaleY", 1f, 1.2f, 1f)

        scaleX.duration = 500 // 애니메이션 지속 시간
        scaleY.duration = 500

        scaleX.repeatCount = ValueAnimator.INFINITE // 무한 반복
        scaleX.repeatMode = ValueAnimator.REVERSE // 커졌다가 다시 작아짐

        scaleY.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatMode = ValueAnimator.REVERSE

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.interpolator = DecelerateInterpolator() // 부드러운 애니메이션 적용
        animatorSet.start() // 애니메이션 시작
    }

    private fun saveToPreferences(value: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("activityPickSave", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selected_icon", value)
        editor.apply() // 비동기적으로 저장
    }

    private fun loadFromPreferences(): String? {
        val sharedPreferences: SharedPreferences = getSharedPreferences("activityPickSave", MODE_PRIVATE)
        return sharedPreferences.getString("selected_icon", null) // 기본값은 null
    }

    private fun loadIcon() {
        val selectedIcon = loadFromPreferences()
        when (selectedIcon) {
            "running" -> {
                binding.ivPick.setImageResource(R.drawable.icon_running100)
                binding.ibRunning.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
            }
            "hiking" -> {
                binding.ivPick.setImageResource(R.drawable.icon_treckking100)
                binding.ibHiking.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
            }
            "trailrunning" -> {
                binding.ivPick.setImageResource(R.drawable.icon_trail100)
                binding.ibTrailrunning.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
            }
            "cycling" -> {
                binding.ivPick.setImageResource(R.drawable.icon_ridding100)
                binding.ibCycling.setBackgroundColor(ContextCompat.getColor(this, R.color.lighter_gray))
            }
            else -> binding.ivPick.setImageResource(R.drawable.logo_sspurt) // 기본 아이콘 설정
        }
    }

    // 모든 버튼의 배경을 초기화하는 메서드
    private fun resetBackgrounds() {
        binding.ibRunning.setBackgroundColor(Color.TRANSPARENT)
        binding.ibHiking.setBackgroundColor(Color.TRANSPARENT)
        binding.ibTrailrunning.setBackgroundColor(Color.TRANSPARENT)
        binding.ibCycling.setBackgroundColor(Color.TRANSPARENT)
    }
}