package com.kisayo.sspurt.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        // 버튼 클릭 시 아이콘 설정
        binding.ibRunning.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_running100)
            saveToPreferences("running")
        }

        binding.ibHiking.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_treckking100)
            saveToPreferences("hiking")
        }

        binding.ibTrailrunning.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_trail100)
            saveToPreferences("trailrunning")
        }

        binding.ibCycling.setOnClickListener {
            binding.ivPick.setImageResource(R.drawable.icon_ridding100)
            saveToPreferences("cycling")
        }

        // ivPick 클릭 시 SharedPreferences에 저장된 값 사용
        binding.ivPick.setOnClickListener {
            // 이미 저장된 값이 있을 경우 사용
            saveToPreferences(loadFromPreferences() ?: "default_value")

            startActivity(Intent(this,GpsConfirmActivity::class.java))
            finish()

        }
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
            "running" -> binding.ivPick.setImageResource(R.drawable.icon_running100)
            "hiking" -> binding.ivPick.setImageResource(R.drawable.icon_treckking100)
            "trailrunning" -> binding.ivPick.setImageResource(R.drawable.icon_trail100)
            "cycling" -> binding.ivPick.setImageResource(R.drawable.icon_ridding100)
            else -> binding.ivPick.setImageResource(R.drawable.logo_sspurt) // 기본 아이콘 설정
        }
    }
}