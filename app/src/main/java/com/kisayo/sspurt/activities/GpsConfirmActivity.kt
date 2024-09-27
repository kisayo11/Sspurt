package com.kisayo.sspurt.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityGpsConfirmBinding
import com.kisayo.sspurt.databinding.ActivityTrackingSaveBinding
import com.kisayo.sspurt.fragments.MapFragment
import org.checkerframework.checker.units.qual.Current

class GpsConfirmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGpsConfirmBinding
    private lateinit var mapFragment: MapFragment
    private lateinit var sharedPreferences: SharedPreferences
    private var countDownTimer: CountDownTimer? = null // Nullable 타입으로 선언
    private var currentTime = 10_000L // 10sec



    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1 // const로 선언
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //sharedPrefernce 초기화
        sharedPreferences = getSharedPreferences("my_preferences", MODE_PRIVATE)
        mapFragment = MapFragment()

        // MapFragment 초기화 및 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commitNow()





        // 플레이 버튼 리스너, 카운트다운 리스너
        binding.ibPlay.setOnClickListener { countDownBtn() }
        binding.tvCountDown.setOnClickListener { countDownBtn() }

        // 카운트다운 초기값설정
        binding.tvCountDown.text = (currentTime / 1000).toString()

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

        binding.ibPause.setOnClickListener {
            startActivity(Intent(this, ActivityTrackingSaveBinding::class.java))

        }



    }// onCreate....

    private fun countDownBtn(){
        if(countDownTimer==null){
            startTimer() // 타이머가 실행 중이 아니면 시작
        } else {
            countDownTimer?.cancel() // 타이머가 실행중이면 취소
            countDownTimer = null // 타이머 종료
            Toast.makeText(this, "타이머 일시 정지", Toast.LENGTH_SHORT).show() // Toast 피드백

        }
    }
    private fun startTimer(){
        countDownTimer = object : CountDownTimer(currentTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = millisUntilFinished
                binding.tvCountDown.text = (currentTime / 1000).toString() // 남은 시간 업데이트

                //카운트다운 에이메이션 효과
                binding.tvCountDown.translationX = (10 - (currentTime / 1000)) * 10f // 이동 효과
                if (currentTime <= 5000) {
                    binding.tvCountDown.setTextColor(resources.getColor(android.R.color.holo_red_light)) // 색상 변경
                } else {
                    binding.tvCountDown.setTextColor(resources.getColor(android.R.color.black)) // 기본 색상
                }
            }

            override fun onFinish() {
                binding.tvCountDown.text = "0" // 카운트다운 완료 시 텍스트 설정
                countDownTimer = null // 타이머 종료


                // ******************여기에 카운트다운 완료 시 실행할 코드 작성**********
            }
        }.start() // 타이머 시작
    }


    //맵 위치 파악 퍼미션
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
