package com.kisayo.sspurt.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityGpsConfirmBinding
import com.kisayo.sspurt.fragments.HealthRecordFragment
import com.kisayo.sspurt.fragments.MapFragment

class GpsConfirmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGpsConfirmBinding
    private lateinit var mapFragment: MapFragment
    private lateinit var healthRecordFragment: HealthRecordFragment
    private lateinit var sharedPreferences: SharedPreferences






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //sharedPrefernce 초기화
        sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE)
        mapFragment = MapFragment()
        healthRecordFragment = HealthRecordFragment()

        // MapFragment 초기화 및 추가
        val exerciseMapFragment = MapFragment.newInstance(true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, exerciseMapFragment)
            .replace(R.id.healthRecord_container, healthRecordFragment)
            .commitNow()

                //맵 표시 스위치 리스너
        binding.mapSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 스위치 상태를 SharedPreferences에 저장
            sharedPreferences.edit().putBoolean("map_visible", isChecked).apply()
            if (isChecked) {
                // MapFragment를 보여줌
                supportFragmentManager.beginTransaction()
                    .show(mapFragment)
                    .commit()
            } else {
                // MapFragment를 숨김
                supportFragmentManager.beginTransaction()
                    .hide(mapFragment)
                    .commit()
            }
        }

        // 이전에 저장된 스위치 상태 불러오기
        val isMapVisible = sharedPreferences.getBoolean("map_visible", false)
        binding.mapSwitch.isChecked = isMapVisible

        // 스위치 상태에 따라 MapFragment 보이기/숨기기
        if (isMapVisible) {
            supportFragmentManager.beginTransaction()
                .show(mapFragment)
                .commit()
        } else { supportFragmentManager.beginTransaction().hide(mapFragment).commit()}

    }
}
