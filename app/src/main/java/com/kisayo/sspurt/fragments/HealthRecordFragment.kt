package com.kisayo.sspurt.fragments

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.kisayo.sspurt.Location.ExerciseTracker
import com.kisayo.sspurt.activities.TrackingSaveActivity
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.databinding.FragmentHealthRecordBinding
import com.kisayo.sspurt.utils.FirestoreHelper
import com.kisayo.sspurt.utils.UserRepository

class HealthRecordFragment : Fragment() {

    private lateinit var binding: FragmentHealthRecordBinding
    private var exerciseData = ExerciseRecord() // 통합된 운동 데이터
    private var recordingTimer: CountDownTimer? = null
    private val firestoreHelper = FirestoreHelper() // FirestoreHelper 인스턴스
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var lastUpdateTime: Long = 0
    private lateinit var healthConnectClient: HealthConnectClient
    private val exerciseTracker = ExerciseTracker() // ExerciseTracker 인스턴스
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHealthRecordBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity()) // 초기화
        healthConnectClient = HealthConnectClient.getOrCreate(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 레코드 버튼(animationview) 클릭 리스너
        binding.recordAni.setOnClickListener {
            binding.countdownAni.visibility = View.VISIBLE
            binding.countdownAni.playAnimation()

            // 애니메이션 끝나면 GONE으로 설정
            binding.countdownAni.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.countdownAni.visibility = View.GONE
                    binding.recordAni.visibility = View.GONE
                    binding.pauseIb.visibility = View.VISIBLE
                    startRecording() // 레코딩 시작
                }
            })
            requestCurrentLocation() // 위치 요청 추가

        }

        // pause 버튼 클릭 리스너
        binding.pauseIb.setOnClickListener {
            if (binding.stopIb.visibility == View.VISIBLE) {
                binding.stopIb.visibility = View.INVISIBLE
            } else {
                binding.stopIb.visibility = View.VISIBLE
            }
            if (exerciseData.isPaused) {
                resumeRecording() // 재개
            } else {
                pauseRecording() // 일시 중지
            }
        }

        // stop 버튼 클릭 리스너
        binding.stopIb.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        binding.stopIb.setOnLongClickListener {
            stopRecording() // 레코딩 중지
            // 애니메이션 및 다음 액티비티로 이동
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 500 // 애니메이션 시간
            animator.addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedValue as Float
                val color = blendColors(Color.BLACK, Color.BLUE, fraction) // 색상 혼합
                binding.stopIb.setColorFilter(color, PorterDuff.Mode.SRC_IN) // 아이콘 색상 업데이트
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val intent = Intent(requireContext(), TrackingSaveActivity::class.java)
                    startActivity(intent)
                }
            })
            animator.start()
            true // 이벤트 소비
        }
    }

    private fun startRecording() {
        exerciseData.isRecording = true
        exerciseData.isPaused = false
        exerciseData.elapsedTime = 0 // 경과 시간 초기화
        startTimer() // 타이머 시작
        //exerciseTracker.startTracking(lastLocation ?: return) // 운동 추적 시작
        requestCurrentLocation() // 현재 위치 요청
        Toast.makeText(requireContext(), "운동이 시작되었습니다!", Toast.LENGTH_SHORT).show() // 운동 시작 알림

        // 권한 요청
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
    }

    private fun pauseRecording() {
        if (exerciseData.isRecording) {
            recordingTimer?.cancel() // 타이머 일시 정지
            exerciseData.isPaused = true // 일시 중지 상태 업데이트
            lastUpdateTime = System.currentTimeMillis() // 일시 중지 시점 저장
        }
    }

    private fun resumeRecording() {
        if (exerciseData.isPaused) {
            startTimer() // 타이머 재개
            exerciseData.isPaused = false // 일시 중지 해제
        }
    }

    private fun stopRecording() {
        try {
            recordingTimer?.cancel()
            exerciseData.isRecording = false
            saveExerciseData() // 데이터 저장 시도
        } catch (e: Exception) {
            Log.e("HealthRecordFragment", "Error in stopRecording: ${e.message}")
            Toast.makeText(requireContext(), "운동 기록 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer() {
        Toast.makeText(requireContext(), "타이머가 시작되었습니다!", Toast.LENGTH_SHORT).show() // 타이머 시작 시 토스트 띄우기

        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                exerciseData.elapsedTime++ // 경과 시간 증가

                // UI 업데이트는 여기서
//                updateUI()
            }

            override fun onFinish() {
                // 타이머 종료 시 처리 (필요 시 구현)
            }
        }.start()
    }

    private fun calculateAverageSpeed(): Double {
        return if (exerciseData.elapsedTime > 0) {
            (exerciseData.distance / exerciseData.elapsedTime) * 3.6 // 평균 속도 (km/h) 계산
        } else {
            0.0
        }
    }

    private fun saveExerciseData() {
        val email = userRepository.getCurrentUserEmail() ?: ""
        val exerciseRecord = ExerciseRecord(
            isRecording = exerciseData.isRecording,
            elapsedTime = exerciseData.elapsedTime,
            distance = exerciseData.distance,
            currentSpeed = exerciseData.currentSpeed,
            averageSpeed = calculateAverageSpeed(), // 평균 속도 계산 추가
            heartHealthScore = exerciseData.heartHealthScore,
            calories = exerciseData.calories,
            temperature = exerciseData.temperature,
            exerciseType = exerciseData.exerciseType,
            userFeedback = exerciseData.userFeedback,
            currentLocation = exerciseData.currentLocation, // LatLng 타입으로 변경 필요
            date = System.currentTimeMillis().toString(),
            photoUrl = exerciseData.photoUrl,
            exerciseJournal = exerciseData.exerciseJournal
        )

        firestoreHelper.saveExerciseRecord(email, exerciseRecord, onSuccess = {
            Toast.makeText(requireContext(), "운동 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }, onFailure = { exception ->
            Toast.makeText(requireContext(), "저장 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
        })
    }

    private fun blendColors(color1: Int, color2: Int, fraction: Float): Int {
        val alpha = (Color.alpha(color1) + fraction * (Color.alpha(color2) - Color.alpha(color1))).toInt()
        val red = (Color.red(color1) + fraction * (Color.red(color2) - Color.red(color1))).toInt()
        val green = (Color.green(color1) + fraction * (Color.green(color2) - Color.green(color1))).toInt()
        val blue = (Color.blue(color1) + fraction * (Color.blue(color2) - Color.blue(color1))).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    private fun requestCurrentLocation() {
        Log.d("LocationRequest", "Entering requestCurrentLocation()")
        Toast.makeText(requireContext(), "위치 요청 중...", Toast.LENGTH_SHORT).show()

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        val locationRequest = createLocationRequest()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationRequest(): LocationRequest {
        Log.d("LocationRequest", "Requesting location updates")
        Toast.makeText(requireContext(), "위치를 잡았습니다.", Toast.LENGTH_SHORT).show()
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setIntervalMillis(1000)
            .build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                // 현재 위치가 업데이트되면
                Log.d("LocationUpdate", "New location: ${location.latitude}, ${location.longitude}")

                // 현재 속도 계산
                val speed = exerciseTracker.calculateCurrentSpeed(location) // m/s
                val speedKmh = speed * 3.6 // km/h로 변환

                // 이동 거리 업데이트
                exerciseTracker.updateDistance(location)

                // 현재 시간 업데이트
                val currentTime = System.currentTimeMillis()
                exerciseTracker.updateTime(currentTime)

                // 평균 속도 계산
                val averageSpeedKmh = exerciseTracker.calculateAverageSpeed() // 인자 없이 호출

                // UI 업데이트
                updateUI(speedKmh, exerciseTracker.getTotalDistance(), averageSpeedKmh)
            }
        }
    }

    private fun updateUI(averageSpeed: Double, totaldistance: Double, currentSpeed: Double) {
        // 현재 속도 가져오기 (km/h로 변환)
        binding.tv4.text = String.format("%.2f", currentSpeed) // 현재 속도

        // 평균 속도 계산
        binding.tv2.text = String.format("%.2f", averageSpeed) // 평균 속도 업데이트

        // 이동 거리 업데이트
        binding.tv1.text = String.format("%.2f", totaldistance) // 이동 거리


        // 이동 시간 포맷팅
        binding.tv3.text = exerciseTracker.formatElapsedTime(exerciseData.elapsedTime) // 이동 시간

        // 칼로리 계산
        exerciseData.calories = exerciseTracker.calculateCalories(exerciseData.elapsedTime)
        binding.tv6.text = String.format("%.2f", exerciseData.calories) // 칼로리

        // 심장 강화 점수 계산
        val heartHealthScore = exerciseTracker.calculateHeartHealthScore(averageSpeed, exerciseData.elapsedTime, exerciseData.calories)
        binding.tv5.text = heartHealthScore.toString() // 심장 강화 점수



    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation() // 권한 허용 시 위치 요청
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}