package com.kisayo.sspurt.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.storage.FirebaseStorage
import com.kisayo.sspurt.Helpers.GpxParser
import com.kisayo.sspurt.Helpers.PolylineHelper
import com.kisayo.sspurt.Helpers.PolylineHelper.getPolylineOptionsByExerciseType
import com.kisayo.sspurt.R
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
    private var isCameraFollowing = true // 카메라 자동 이동 상태
    private var activeMarker: Marker? = null




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
                    val latLng = LatLng(location.latitude, location.longitude)

                    // 위치 업데이트가 있을 때 카메라를 이동할지 결정
                    if (isCameraFollowing) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    }
                    // ViewModel의 경로 업데이트
                    recordViewModel.addRoutePoint(latLng)
                }
            }
        }


        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,10000
        ).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없으면 권한 요청
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // 권한이 있는 경우에만 위치 업데이트 요청
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
        return view
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 카메라 이동 상태 제어
        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                // 사용자가 지도를 움직인 경우 카메라 따라가지 않도록 설정
                isCameraFollowing = false
            }
        }

        // 폴리라인 클릭 리스너
        mMap.setOnPolylineClickListener { clickedPolyline ->
            val tagData = clickedPolyline.tag as? Map<String, Any> ?: return@setOnPolylineClickListener

            // 태그에서 필요한 데이터 가져오기
            val coordinates = tagData["coordinates"] as? List<LatLng> ?: return@setOnPolylineClickListener
            val mountainName = tagData["mountainName"] as? String ?: return@setOnPolylineClickListener
            val highestElevation = tagData["highestElevation"] as? Int ?: return@setOnPolylineClickListener
            val totalDistance = tagData["totalDistance"] as? Double ?: return@setOnPolylineClickListener

            // 이전 마커가 있으면 제거
            activeMarker?.remove()

            // 시작 지점에 마커 추가
            activeMarker = mMap.addMarker(
                MarkerOptions()
                    .position(coordinates.first()) // 시작 지점
                    .title(mountainName)
                    .snippet("${highestElevation}m / ${"%.1f".format(totalDistance)}km")
            )

            // 인포 윈도우를 자동으로 보여줌
            activeMarker?.showInfoWindow()
        }

        // 지도 클릭 리스너 설정 (다른 곳 클릭 시 마커 제거)
        mMap.setOnMapClickListener {
            // 마커가 있다면 제거
            activeMarker?.remove()
            activeMarker = null // 활성화된 마커 초기화
        }


        mMap.setOnCameraIdleListener {
            val visibleRegion = mMap.projection.visibleRegion.latLngBounds

            // 가시성 영역의 북동쪽과 남서쪽 좌표를 가져옵니다.
            val northEast = visibleRegion.northeast
            val southWest = visibleRegion.southwest

            // GPX 파일 로드 함수 호출
            loadGpxFiles(northEast, southWest)
        }

        // `isExerciseMap`에 따라 다르게 동작 설정
        if (isExerciseMap) {
            // `exercisemap` 모드인 경우
            setupPolyline() // 폴리라인 설정
            setupMap()
            observeRecordingState()
        } else {
            // `nearby` 모드인 경우 GPX 파일 로드
            setupMap()
            loadGpxFromFirebase() // GPX 파일을 Firebase에서 불러오기

        }
    }

    private fun loadGpxFiles(northEast: LatLng, southWest: LatLng) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("gpxfiles/")

        storageRef.listAll().addOnSuccessListener { listResult ->
            for (item in listResult.items) {
                if (item.name.endsWith(".gpx")) {
                    item.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                        val inputStream = bytes.inputStream()
                        val gpxParser = GpxParser()
                        val (coordinates, elevationData) = gpxParser.parse(inputStream)

                        // 산 이름, 고도 및 거리 정보 추출
                        val mountainName = item.name.substringBefore("_") // 파일 이름에서 산 이름 추출
                        val highestElevation = elevationData.first
                        val totalDistance = elevationData.second

                        // 폴리라인 그리기 및 클릭 리스너 설정
                        drawPolylineOnMap(coordinates, mountainName, highestElevation, totalDistance)
                    }.addOnFailureListener { exception ->
                        Log.e("MapFragment", "Failed to load GPX file: ${exception.message}")
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapFragment", "Failed to list GPX files: ${exception.message}")
        }
    }

//    private fun addMarkers(coordinates: List<LatLng>, highestElevation: Int, totalDistance: Double, fileName: String) {
//        if (coordinates.isNotEmpty()) {
//            val startPoint = coordinates.first() // 시작점 좌표
//            val mountainName = fileName.substringBefore("_") // 파일 이름에서 산 이름 추출
//
//            // 마커 추가
//            mMap.addMarker(
//                MarkerOptions()
//                    .position(startPoint)
//                    .title(mountainName)
//                    .snippet("${highestElevation}m / ${"%.1f".format(totalDistance)}km")
//            )
//        }
//    }

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
            if (isExerciseMap && isRecording) {
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
        polylineOptions = PolylineHelper.getPolylineOptionsByExerciseType(currentExerciseType)

        // 폴리라인 추가
        polyline = mMap.addPolyline(polylineOptions)

        // 위치 업데이트 콜백 설정
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (isExerciseMap) {
                    for (location in locationResult.locations) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        if (shouldAddPoint(latLng)) {
                            recordViewModel.addRoutePoint(latLng)
                            val points = polyline.points
                            points.add(latLng)
                            polyline.points = points
                        }
                    }
                }
            }
        }
    }


    private fun shouldAddPoint(latLng: LatLng): Boolean {
        // 간격에 따른 필터링 로직 구현 (예: 시간, 거리, 위치 변경량 등을 기준으로)
        // 예시로 간단하게 타이머 또는 거리 차이로 필터링할 수 있습니다.
        // return true만 추가해도 기본적으로 작동됩니다.
        return true
    }

    private fun loadGpxFromFirebase() {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("gpxfiles") // gpxfiles 폴더 참조

        // 폴더 내의 모든 파일 목록 가져오기
        storageRef.listAll().addOnSuccessListener { listResult ->
            // 최상위 파일 처리
            for (item in listResult.items) {
                // 각 파일의 다운로드 URL을 가져와서 GPX 파일 다운로드
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                    val inputStream = bytes.inputStream()
                    val gpxParser = GpxParser()
                    val (coordinates, elevationData) = gpxParser.parse(inputStream) // GPX 파싱

                    drawPolylineOnMap(coordinates) // 폴리라인 그리기
//                    addMarkers(coordinates, elevationData.first, elevationData.second, item.name)
                }.addOnFailureListener { exception ->
                    Log.e("MapFragment", "Failed to load GPX file: ${exception.message}")
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapFragment", "Failed to list files: ${exception.message}")
        }
    }

    private fun drawPolylineOnMap(coordinates: List<LatLng>) {
        val simplifiedCoordinates = PolylineHelper.simplifyCoordinates(coordinates)
        val polylineOptions = PolylineHelper.getPolylineOptionsByExerciseType(getExerciseTypeFromPreferences())
            .addAll(simplifiedCoordinates)
        mMap.addPolyline(polylineOptions)
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
    private fun drawPolylineOnMap(coordinates: List<LatLng>, mountainName: String, highestElevation: Int, totalDistance: Double) {
        val simplifiedCoordinates = PolylineHelper.simplifyCoordinates(coordinates)
        val polylineOptions = PolylineHelper.getPolylineOptionsByExerciseType(getExerciseTypeFromPreferences())
            .addAll(simplifiedCoordinates)

        val polyline = mMap.addPolyline(polylineOptions)
        polyline.isClickable = true
        // 태그로 폴리라인의 식별 정보를 저장
        polyline.tag = mapOf("coordinates" to coordinates, "mountainName" to mountainName, "highestElevation" to highestElevation, "totalDistance" to totalDistance)
    }

}