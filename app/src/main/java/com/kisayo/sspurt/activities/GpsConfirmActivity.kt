package com.kisayo.sspurt.activities

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityGpsConfirmBinding
import com.kisayo.sspurt.fragments.MapFragment

class GpsConfirmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGpsConfirmBinding
    private lateinit var mapFragment: MapFragment
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1 // const로 선언
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE)
        mapFragment = MapFragment()

        // 이전에 저장된 스위치 상태 불러오기
        val isMapVisible = sharedPreferences.getBoolean("map_visible", false)
        binding.mapSwitch.isChecked = isMapVisible

        // MapFragment 초기화 및 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commitNow()

        // 스위치 상태에 따라 MapFragment 보이기/숨기기
        if (isMapVisible) {
            supportFragmentManager.beginTransaction()
                .show(mapFragment)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .hide(mapFragment)
                .commit()
        }

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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되었을 때
                mapFragment.onResume() // Fragment를 다시 시작
            } else {
                // 권한 거부 처리
            }
        }
    }
}
