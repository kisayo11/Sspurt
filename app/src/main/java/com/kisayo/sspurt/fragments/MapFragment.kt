package com.kisayo.sspurt.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.utils.RecordViewModel
import com.kisayo.sspurt.utils.UserRepository

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var polyline: Polyline
    private lateinit var polylineOptions: PolylineOptions
    private val recordViewModel: RecordViewModel by activityViewModels()
    private lateinit var userRepository: UserRepository // UserRepository 추가
    private var isExerciseMap: Boolean = false // 모드 구분 변수


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // `Bundle`에서 `isExerciseMap` 값 가져오기
        isExerciseMap = arguments?.getBoolean("IS_EXERCISE_MAP") ?: false

        // SupportMapFragment를 가져와서 비동기로 지도 준비
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // UserRepository 초기화
        userRepository = UserRepository(requireContext())

        // LocationCallback 초기화
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // 위치 업데이트가 있을 때마다 카메라를 이동
                    val latLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                    // ViewModel의 경로 업데이트
                    recordViewModel.addRoutePoint(latLng) // 이 부분에 추가

                }
            }
        }

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        // `isExerciseMap`에 따라 다르게 동작 설정
        if (isExerciseMap) {
            // `exercisemap` 모드인 경우
            setupPolyline() // 폴리라인 설정
            setupMap()
            observeRecordingState()
        } else {
            // `nearby` 모드인 경우
            setupMap()
        }
    }


    // 폴리라인을 설정하는 함수
    private fun setupPolyline() {
        // 운동 타입에 따른 폴리라인 설정
        val currentExerciseType = getExerciseTypeFromPreferences()
        polylineOptions = getPolylineOptionsByExerciseType(currentExerciseType)
        polyline = mMap.addPolyline(polylineOptions)
    }

    // 녹화 상태에 따른 폴리라인 관리
    private fun observeRecordingState() {
        recordViewModel.isRecording.observe(viewLifecycleOwner, Observer { isRecording ->
            if (isRecording) {
                startDrawingPolyline()
            } else {
                hidePolyline()
            }
        })
    }

    // 지도에 폴리라인을 그리기 시작하는 함수
    private fun startDrawingPolyline() {
        // `SharedPreferences`에서 운동 타입 가져오기
        val currentExerciseType = getExerciseTypeFromPreferences()

        // 운동 타입에 따른 폴리라인 옵션 생성
        val polylineOptions = getPolylineOptionsByExerciseType(currentExerciseType)

        // 폴리라인 추가
        polyline = mMap.addPolyline(polylineOptions)

        // 위치 업데이트 콜백 설정
        if (isExerciseMap) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        // 새로운 좌표를 폴리라인에 추가
                        val latLng = LatLng(location.latitude, location.longitude)
                        val points = polyline.points
                        points.add(latLng)
                        polyline.points = points
                    }
                }
            }
        }
    }

    // 폴리라인 그리기 중단
    private fun stopDrawingPolyline() {
        if (isExerciseMap) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }



    private fun setupMap() {
        // 현재 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // 기본 UI 설정
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true
        mMap.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true

        // 현재 위치 요청
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
            }
        }
    }

    private fun fetchRouteFromViewModel() {
        val email = userRepository.getCurrentUserEmail()

        if (email.isNullOrEmpty()) {
            Log.e("MapFragment", "User email is null or empty.")
            return
        }

        // ViewModel을 통해 경로 데이터 가져오기
        recordViewModel.fetchRoute(email, requireContext())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                setupMap()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        // MapFragment의 인스턴스를 생성할 때 `isExerciseMap` 전달
        fun newInstance(isExerciseMap: Boolean): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putBoolean("IS_EXERCISE_MAP", isExerciseMap)
            fragment.arguments = args
            return fragment
        }

        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


    private fun getPolylineOptionsByExerciseType(exerciseType: String): PolylineOptions {
        return when (exerciseType) {
            "running" -> PolylineOptions()
                .color(Color.BLUE) // 달리기 경로를 파란색으로 표시
                .width(12f)
                .geodesic(true)
                .jointType(JointType.ROUND)

            "cycling" -> PolylineOptions()
                .color(Color.GREEN) // 자전거 경로를 초록색으로 표시
                .width(10f)
                .geodesic(true)
                .pattern(listOf(Dash(30f), Gap(20f)))

            "hiking" -> PolylineOptions()
                .color(Color.rgb(139, 69, 19)) // 등산 경로를 갈색으로 표시
                .width(15f)
                .geodesic(true)
                .pattern(listOf(Dash(10f), Gap(10f)))

            "trailrunning" -> PolylineOptions()
                .color(Color.MAGENTA) // 트레일런닝 경로를 마젠타로 표시
                .width(14f)
                .geodesic(true)
                .jointType(JointType.ROUND)

            else -> PolylineOptions()
                .color(Color.GRAY) // 기본 색상
                .width(10f)
        }
    }

    private fun getExerciseTypeFromPreferences(): String {
        // `SharedPreferences`에서 `activityPickSave`에 저장된 운동 타입 가져오기
        val sharedPreferences = requireContext().getSharedPreferences("activityPickSave", Context.MODE_PRIVATE)
        // 저장된 운동 타입을 가져오고, 없을 경우 기본값은 "running"
        return sharedPreferences.getString("selected_icon", "running") ?: "running"
    }

    private fun hidePolyline() {
        if (this::polyline.isInitialized) {
            polyline.remove()
        }
    }



}