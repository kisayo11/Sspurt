package com.kisayo.sspurt.fragments

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Timestamp
import com.kisayo.sspurt.Helpers.FirestoreHelper
import com.kisayo.sspurt.Location.ExerciseTracker
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.TrackingSaveActivity
import com.kisayo.sspurt.activities.TrackingService
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.data.LatLngWrapper
import com.kisayo.sspurt.data.RealTimeData
import com.kisayo.sspurt.databinding.FragmentHealthRecordBinding
import com.kisayo.sspurt.utils.RecordViewModel
import com.kisayo.sspurt.utils.UserRepository

class HealthRecordFragment : Fragment() {

    private lateinit var binding: FragmentHealthRecordBinding
    private var exerciseData = ExerciseRecord() // 통합된 운동 데이터
    private var recordingTimer: CountDownTimer? = null
    private val firestoreHelper = FirestoreHelper() // FirestoreHelper 인스턴스
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastUpdateTime: Long = 0
    private lateinit var healthConnectClient: HealthConnectClient
    private val exerciseTracker = ExerciseTracker() // ExerciseTracker 인스턴스
    private lateinit var userRepository: UserRepository
    private var previousLocation: Location? = null // 이전 위치
    private var totalDistance: Double = 0.0 // 총 이동 거리
    private var exerciseType: String? = null // 운동 유형을 저장할 변수
    private var lastAltitude: Double = 0.0 // 최근 고도 값
    private var realTimeData = RealTimeData() // 실시간 데이터
    private val recordViewModel: RecordViewModel by activityViewModels()
    private var isAnimationFinished = false // 애니메이션 완료 상태 플래그
    private val snapShot: String? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHealthRecordBinding.inflate(inflater, container, false)
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity()) // 초기화
        healthConnectClient = HealthConnectClient.getOrCreate(requireContext())
        userRepository = UserRepository(requireContext())
        exerciseData = ExerciseRecord() // 초기화

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Android 13 이상에서 POST_NOTIFICATIONS 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionResult = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }


        recordViewModel.isRecording.observe(viewLifecycleOwner, Observer { isRecording ->
            if (isRecording) {
                resumeRecording() // 녹화 재개
            } else {
                // 녹화가 아닌 경우에도, 일시 정지 상태를 체크하여 잘못된 종료를 방지
                if (recordViewModel.isPaused.value == false && exerciseData.isRecording) {
                    stopRecording() // 녹화 중지
                }
            }
        })

        recordViewModel.isPaused.observe(viewLifecycleOwner, Observer { isPaused ->
            if (isPaused) {
                pauseRecording()
            }
        })


        // 레코드 버튼(animationview) 클릭 리스너
        binding.recordAni.setOnClickListener {
            binding.recordAni.isEnabled = false // 버튼 비활성화
            isAnimationFinished = false // 애니메이션 시작 시 플래그 초기화

            // 카운트다운 애니메이션 시작 시 배경 어둡게 처리
            val window = requireActivity().window
            val layoutParams = window.attributes
            layoutParams.dimAmount = 0.5f // 배경을 어둡게 처리
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = layoutParams

            binding.countdownAni.visibility = View.VISIBLE
            binding.countdownAni.playAnimation()

            // 애니메이션 끝나면 GONE으로 설정
            binding.countdownAni.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.countdownAni.visibility = View.GONE
                    binding.recordAni.visibility = View.GONE
                    binding.pauseIb.visibility = View.VISIBLE
                    isAnimationFinished = true // 애니메이션 완료

                    // 배경을 다시 밝게 처리
                    layoutParams.dimAmount = 0f
                    window.attributes = layoutParams

                    //Foreground Service 시작
                    startTrackingService()
                    startRecording() // 레코딩 시작
                    recordViewModel.startRecording() // ViewModel에서 레코딩 시작
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
                recordViewModel.startRecording() // ViewModel에서 재개

            } else {
                pauseRecording() // 일시 중지
                recordViewModel.setPaused(true) // ViewModel에서 일시 정지 상태 설정

            }

        }

        // stop 버튼 클릭 리스너
        binding.stopIb.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        binding.stopIb.setOnLongClickListener {
            stopRecording() // 레코딩 중지
            recordViewModel.stopRecording() // ViewModel에서 레코딩 중지

            // Foreground Service 중지
            stopTrackingService()

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

                }
            })
            animator.start()
            true // 이벤트 소비
        }
    }

    private fun startRecording() {
        if (!isAnimationFinished) return // 애니메이션이 완료되지 않았다면 실행 안함

        exerciseData.isRecording = true
        exerciseData.isPaused = false

        // ViewModel 상태 확실히 설정
        recordViewModel.startRecording()

        exerciseData.elapsedTime = 0 // 경과 시간 초기화

        exerciseData.exerciseType = exerciseType ?: "" // 운동 유형 설정 (null일 경우 빈 문자열로 초기화)

        startTimer() // 타이머 시작
        exerciseData.metValue =
            exerciseTracker.getMetValue(exerciseData.exerciseType) // 운동 유형에 따라 MET 값 설정

        requestCurrentLocation() // 현재 위치 요청
        Toast.makeText(requireContext(), "운동이 시작되었습니다!", Toast.LENGTH_SHORT).show() // 운동 시작 알림

        // 권한 요청
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }
    }

    private fun pauseRecording() {
        if (exerciseData.isRecording) {
            recordingTimer?.cancel() // 타이머 일시 정지
            exerciseData.isPaused = true // 일시 중지 상태 업데이트
            lastUpdateTime = System.currentTimeMillis() // 일시 중지 시점 저장
            fusedLocationClient.removeLocationUpdates(locationCallback) // 위치 업데이트 중단
        }
    }

    private fun resumeRecording() {
        if (exerciseData.isPaused) {
            exerciseData.isPaused = false // 일시 중지 해제
            startTimer() // 타이머 재개
            previousLocation = null // 이전 위치 초기화, 새로 시작할 때까지 대기
            requestCurrentLocation() // 위치 요청 재개
        }
    }

    private fun stopRecording() {
        try {
            recordingTimer?.cancel() // 타이머 중지
            exerciseData.isRecording = false
            fusedLocationClient.removeLocationUpdates(locationCallback) // 위치 업데이트 중단

            // MapFragment에서 캡처 기능 호출
            val mapFragment = parentFragmentManager.findFragmentById(R.id.map_fragment_container) as? MapFragment
            mapFragment?.capturePolylineOnly {
            // 폴리라인 캡처 완료 후 지도 배경과 함께 캡처
            mapFragment.captureMapWithPolyline {
            // 모든 캡처 작업 완료 후 데이터 저장
            saveExerciseData()
                }
            }
        } catch (e: Exception) {
            Log.e("HealthRecordFragment", "Error in stopRecording: ${e.message}")
            Toast.makeText(requireContext(), "운동 기록 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer() {
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                exerciseData.elapsedTime++ // 경과 시간 증가
                updateUI() // UI 업데이트 호출
            }

            override fun onFinish() {
                // 타이머 종료 시 처리 (필요 시 구현)
            }
        }.start()
    }

    private fun calculateAverageSpeed(): Double {
        return if (exerciseData.elapsedTime > 0) {
            val averageSpeedInKmPerMin =
                (exerciseData.distance / 1000) / (exerciseData.elapsedTime / 60.0)
            averageSpeedInKmPerMin // 분속으로 반환
        } else {
            0.0
        }
    }

    private fun updateRealTimeData(altitude: Double, incline: Double, decline: Double) {
        // RealTimeData 인스턴스 생성
        val realTimeData = RealTimeData(
            altitude = altitude, incline = incline, decline = decline
        )

        // ExerciseData에 실시간 데이터 저장
        exerciseData.realTimeData = realTimeData

        // 데이터 저장 호출
        //saveExerciseData() // 데이터를 저장
    }


    private fun saveExerciseData() {
        val email = userRepository.getCurrentUserEmail() ?: ""
        val currentLocation = exerciseData.currentLocation?.let {
            LatLngWrapper(it.latitude, it.longitude)
        }
        val sharedPreferences =
            requireContext().getSharedPreferences("activityPickSave", Context.MODE_PRIVATE)
        val savedExerciseType = sharedPreferences.getString("selected_icon", "")
        exerciseData.exerciseType = savedExerciseType!!

        exerciseData.capturePolylineOnly = recordViewModel.polylineCapturePath.value
        exerciseData.captureMapWithPolyline = recordViewModel.mapWithPolylineCapturePath.value


        // Firestore에 저장할 exerciseRecord
        val exerciseRecord = ExerciseRecord(
            exerciseRecordId = exerciseData.exerciseRecordId,
            isRecording = exerciseData.isRecording,
            isPaused = exerciseData.isPaused,
            elapsedTime = exerciseData.elapsedTime,
            distance = exerciseData.distance,
            currentSpeed = exerciseData.currentSpeed,
            averageSpeed = calculateAverageSpeed(),
            maxSpeed = exerciseData.maxSpeed,
            heartHealthScore = exerciseData.heartHealthScore,
            calories = exerciseData.calories,
            temperature = exerciseData.temperature,
            exerciseType = exerciseData.exerciseType,
            userFeedback = exerciseData.userFeedback,
            currentLocation = currentLocation,
            date = Timestamp.now(),
            photoUrl = exerciseData.photoUrl,
            exerciseJournal = exerciseData.exerciseJournal,
            metValue = exerciseData.metValue,
            realTimeData = exerciseData.realTimeData,
            isShared = exerciseData.isShared,
            locationTag = exerciseData.locationTag,
            routes = exerciseData.routes,
            ownerEmail = email,
            capturePolylineOnly =  exerciseData.capturePolylineOnly,
            captureMapWithPolyline = exerciseData.captureMapWithPolyline

        )

        firestoreHelper.saveExerciseRecord(email, exerciseRecord, onSuccess = { generatedId ->
            exerciseData.exerciseRecordId = generatedId // Firestore에서 생성된 ID를 저장
            Toast.makeText(requireContext(), "운동 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()

            // exerciseRecordId가 설정된 후 Intent를 생성하고 데이터를 전달
            val intent = Intent(requireContext(), TrackingSaveActivity::class.java).apply {
                putExtra("exerciseRecordId", exerciseData.exerciseRecordId) // 설정된 ID 전달
                putExtra("sourceFragment", "HealthRecord") // 출처 정보 전달
            }
            startActivity(intent) // Activity 시작

        }, onFailure = { exception ->
            Toast.makeText(requireContext(), "저장 실패: ${exception.message}", Toast.LENGTH_SHORT)
                .show()
        })
    }

    private fun blendColors(color1: Int, color2: Int, fraction: Float): Int {
        val alpha =
            (Color.alpha(color1) + fraction * (Color.alpha(color2) - Color.alpha(color1))).toInt()
        val red = (Color.red(color1) + fraction * (Color.red(color2) - Color.red(color1))).toInt()
        val green =
            (Color.green(color1) + fraction * (Color.green(color2) - Color.green(color1))).toInt()
        val blue =
            (Color.blue(color1) + fraction * (Color.blue(color2) - Color.blue(color1))).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    private fun requestCurrentLocation() {
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }
        //위치요청 생성
        val locationRequest = createLocationRequest()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationRequest(): LocationRequest {
        Log.d("LocationRequest", "Requesting location updates")
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setIntervalMillis(1000)
            .setMinUpdateDistanceMeters(5f)
            .build()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.locations.isNotEmpty()) {
                val currentLocation = locationResult.locations.last()

                // 최소 이동 거리 설정 (예: 5미터)
                val MIN_DISTANCE_THRESHOLD = 5.0 // 미터 단위

                exerciseData.currentLocation =
                    LatLngWrapper(currentLocation.latitude, currentLocation.longitude)

                // LatLng 객체 생성
                val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)

                // ViewModel에 좌표 추가
                recordViewModel.addRoutePoint(latLng)

                // `routes` 리스트에 현재 위치 추가
                exerciseData.routes = exerciseData.routes + LatLngWrapper(
                    currentLocation.latitude, currentLocation.longitude
                )


                // 이동 거리 계산
                // 일시 정지 상태에서 거리 계산을 하지 않음
                if (!exerciseData.isPaused) {
                    previousLocation?.let {
                        val distance = it.distanceTo(currentLocation) // 두 위치 간의 거리 (미터)

                        // 최소 이동 거리 설정 (예: 5미터)
                        val MIN_DISTANCE_THRESHOLD = 5.0 // 미터 단위

                        // 이동 거리가 5미터 미만이면 무시
                        if (distance >= MIN_DISTANCE_THRESHOLD) {
                            totalDistance += distance // 총 이동 거리 업데이트
                            exerciseData.distance = totalDistance // ExerciseData에 총 거리 업데이트
                        }
                    }
                    previousLocation = currentLocation
                } else {
                    previousLocation = currentLocation
                }


                // 현재 속도 계산
                val speed = currentLocation.speed // m/s
                val speedKmh = currentLocation.speed * 3.6 // km/h로 변환

                // 최소, 최대 속도 필터링 (예: 0km/h ~ 60km/h 범위)
                if (speedKmh in 0.0..60.0) {
                    exerciseData.currentSpeed = speedKmh.toDouble()

                    // 최고 속도 갱신
                    if (speedKmh > exerciseData.maxSpeed) {
                        exerciseData.maxSpeed = speedKmh.toDouble()
                    }
                }

                // 현재 속도를 분속으로 변환 (min/km로 변환)
                val currentPace = if (speedKmh > 0.1) 60 / speedKmh else 0.0 // 너무 낮은 속도는 무시
                val paceMinutes = currentPace.toInt() // 분
                val paceSeconds = ((currentPace - paceMinutes) * 60).toInt() // 초

// UI 업데이트
                binding.tv4.text = if (speedKmh > 0.1) {
                    String.format("%d' %02d\"", paceMinutes, paceSeconds)
                } else {
                    "0' 00\"" // 너무 낮은 속도일 때는 0' 00"로 표시
                }


                // 이동 거리 및 평균 속도 업데이트
                updateUI() // 이동 거리 및 평균 속도 업데이트 호출

                // 고도, 인클라인, 디클라인 업데이트
                val newAltitude = currentLocation.altitude // 현재 고도 가져오기
                val newIncline = 0.0 // 인클라인 계산
                val newDecline = 0.0 // 디클라인 계산

                // 5미터 이상 상승 시 업데이트
                if (Math.abs(newAltitude - lastAltitude) >= 5) {
                    updateRealTimeData(newAltitude, newIncline, newDecline)
                    lastAltitude = newAltitude // 마지막 고도 값 업데이트
                }
            }
        }
    }


    private fun updateUI() {
        val totalDistanceInKm = totalDistance / 1000.0
        binding.tv1.text = String.format("%.2f", totalDistanceInKm) // 이동 거리

        // 총 시간 계산
        val totalTimeInSeconds = exerciseData.elapsedTime // elapsedTime을 초 단위로 가정

        // 평균 속도 계산 (분속)
        val averageSpeedPerMinute =
            if (totalTimeInSeconds > 0) totalDistance / totalTimeInSeconds else 0.0

        // 평균 속도를 "분'초"" 형식으로 변환
        val paceMinutes = (averageSpeedPerMinute * 60).toInt() // 분
        val paceSeconds = ((averageSpeedPerMinute * 60) % 60).toInt() // 초

        // UI 업데이트
        binding.tv2.text = String.format("%d' %02d\"", paceMinutes, paceSeconds) // 평균 속도

        binding.tv3.text = exerciseTracker.formatElapsedTime(exerciseData.elapsedTime) // 이동 시간

        // 칼로리 계산
        val calories =
            exerciseTracker.calculateCalories(exerciseData.elapsedTime, exerciseData.exerciseType)
        exerciseData.calories = calories
        binding.tv6.text = String.format("%.2f", exerciseData.calories)

        val heartHealthScore = exerciseTracker.calculateHeartHealthScore(
            exerciseData.averageSpeed,
            exerciseData.elapsedTime,
            exerciseData.calories,
            exerciseData.metValue // MET 값 전달
        )
        binding.tv5.text = heartHealthScore.toString() // 심장 강화 점수
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCurrentLocation() // 권한 허용 시 위치 요청
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTrackingService() {
        val startIntent = Intent(requireContext(), TrackingService::class.java)
        startIntent.action = "ACTION_START" // 일반 서비스 시작
        requireContext().startService(startIntent) // 일반 서비스로 시작
    }

    private fun stopTrackingService() {
        val stopIntent = Intent(requireContext(), TrackingService::class.java)
        stopIntent.action = "ACTION_STOP" // 서비스 종료
        requireContext().startService(stopIntent) // 서비스 중지

    }

    override fun onResume() {
        super.onResume()
        // 녹화 중이면서 일시 정지 상태가 아닌 경우에만 재개
        if (recordViewModel.isRecording.value == true && recordViewModel.isPaused.value == false) {
            resumeRecording() // UI 업데이트 및 타이머 재개
        }
    }

    }
