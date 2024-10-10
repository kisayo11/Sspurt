package com.kisayo.sspurt.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.google.android.material.appbar.AppBarLayout
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
    private lateinit var userRepository: UserRepository
    private var isExerciseMap: Boolean = false
    private var isCameraFollowing = true
    private var activeMarker: Marker? = null
    private var isRequestingLocationUpdates = false
    private var activeBorderPolyline: Polyline? = null
    private val polylineList = mutableListOf<Pair<String, Polyline>>()  // Pair(산 이름, Polyline)



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // 전달받은 인자 확인
        isExerciseMap = arguments?.getBoolean("IS_EXERCISE_MAP", false) ?: false

        // isExerciseMap 값이 true일 경우 (GpsConfirmActivity에서 사용됨) 앱바 숨기기
        if (isExerciseMap) {
            val appBarLayout = activity?.findViewById<AppBarLayout>(R.id.appbar_layout)
            appBarLayout?.visibility = View.GONE
        }

        // SupportMapFragment를 가져와서 비동기로 지도 준비
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // LocationCallback 초기화
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location.accuracy < 50) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        if (isCameraFollowing) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                        }
                        recordViewModel.addRoutePoint(latLng)
                    } else {
                        Log.w("MapFragment", "Dropped invalid location with low accuracy: ${location.accuracy}")
                    }
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .apply { setMinUpdateIntervalMillis(5000) }
            .build()

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            if (!isRequestingLocationUpdates) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                isRequestingLocationUpdates = true
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // UserRepository 초기화
        userRepository = UserRepository(requireContext())

        val searching = requireActivity().findViewById<EditText>(R.id.searching)

        searching.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, afer: Int) {
                val query = s.toString()
                searchPolylineAndMoveCamera(query)
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap



        // 사용자가 지도를 움직이면 카메라 이동 멈춤
        mMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isCameraFollowing = false
            }
        }

        // 폴리라인 클릭 리스너 설정
        mMap.setOnPolylineClickListener { clickedPolyline ->
            val tagData = clickedPolyline.tag as? Map<String, Any> ?: return@setOnPolylineClickListener
            val coordinates = tagData["coordinates"] as? List<LatLng> ?: return@setOnPolylineClickListener
            val mountainName = tagData["mountainName"] as? String ?: return@setOnPolylineClickListener
            val highestElevation = tagData["highestElevation"] as? Int ?: return@setOnPolylineClickListener
            val totalDistance = tagData["totalDistance"] as? Double ?: return@setOnPolylineClickListener

            // 이전에 선택된 폴리라인 테두리가 있다면 제거
            activeBorderPolyline?.remove()

            val borderPolylineOptions = PolylineOptions()
                .addAll(coordinates)
                .color(Color.rgb(57, 255, 20))  // 형광색 테두리 (네온 그린)
                .width(25f)  // 테두리 두께 (원하는 두께로 조정)

            // 새로운 테두리 폴리라인 추가 및 저장
            activeBorderPolyline = mMap.addPolyline(borderPolylineOptions)

            // 기존의 폴리라인을 다시 그려서 테두리 위에 겹치게 설정 (기존의 스타일 유지)
            val foregroundPolylineOptions = PolylineOptions()
                .addAll(coordinates)
                .color(clickedPolyline.color)  // 기존 폴리라인의 색상 유지
                .width(10f)  // 기존 폴리라인 두께

            mMap.addPolyline(foregroundPolylineOptions)

            // 이전 마커 제거 후 새로운 마커 추가
            activeMarker?.remove()
            activeMarker = mMap.addMarker(
                MarkerOptions()
                    .position(coordinates.first())
                    .title(mountainName)
                    .snippet("${highestElevation}m / ${"%.1f".format(totalDistance)}km")
            )
            activeMarker?.showInfoWindow()
        }

        // 지도 클릭 리스너 설정 (다른 곳 클릭 시 마커 및 테두리 제거)
        mMap.setOnMapClickListener {
            // 선택된 폴리라인 테두리가 있다면 제거
            activeBorderPolyline?.remove()
            activeBorderPolyline = null // 선택된 테두리 초기화

            // 마커가 있다면 제거
            activeMarker?.remove()
            activeMarker = null // 활성화된 마커 초기화
        }

        // 카메라가 멈춘 후 가시 영역 내의 GPX 파일을 로드
        mMap.setOnCameraIdleListener {
            val visibleRegion = mMap.projection.visibleRegion.latLngBounds
            loadGpxFiles(visibleRegion.northeast, visibleRegion.southwest)
        }

        // 지도 설정 (운동 모드에 따라 다르게 설정)
        if (isExerciseMap) {
            setupPolyline()
            setupMap()
            observeRecordingState()
        } else {
            setupMap()
            loadGpxFromFirebase()
        }
    }

    // GPX 파일을 Firebase에서 로드하는 함수
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
                        val mountainName = item.name.substringBefore("_")
                        val highestElevation = elevationData.first
                        val totalDistance = elevationData.second
                        val exerciseType = extractExerciseTypeFromFileName(item.name)
                        drawPolylineOnMap(coordinates, mountainName, highestElevation, totalDistance, exerciseType)
                    }.addOnFailureListener { exception ->
                        Log.e("MapFragment", "Failed to load GPX file: ${exception.message}")
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapFragment", "Failed to list GPX files: ${exception.message}")
        }
    }

    // 파일 이름에서 운동 타입 추출
    private fun extractExerciseTypeFromFileName(fileName: String): String {
        return fileName.substringBeforeLast(".gpx").substringAfter("_")
    }

    // 지도 설정 함수
    private fun setupMap() {
        // 현재 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // 기본 UI 설정
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true
        mMap.uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true

        // 현재 위치로 카메라 이동
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
            }
        }
    }

    // 폴리라인을 그리는 함수
    private fun drawPolylineOnMap(coordinates: List<LatLng>, mountainName: String, highestElevation: Int, totalDistance: Double, exerciseType: String?) {
        val simplifiedCoordinates = PolylineHelper.simplifyCoordinates(coordinates)
        val polylineOptions = PolylineHelper.getPolylineOptionsByExerciseType(exerciseType ?: getExerciseTypeFromPreferences())
            .addAll(simplifiedCoordinates)
        val polyline = mMap.addPolyline(polylineOptions)
        polyline.isClickable = true
        polylineList.add(Pair(mountainName, polyline)) // 폴리라인정보를 리스트에 저장
        polyline.tag = mapOf("coordinates" to coordinates, "mountainName" to mountainName, "highestElevation" to highestElevation, "totalDistance" to totalDistance)
    }

    // 운동 모드에 따른 폴리라인 설정 함수
    private fun setupPolyline() {
        val currentExerciseType = getExerciseTypeFromPreferences()
        polylineOptions = PolylineHelper.getPolylineOptionsByExerciseType(currentExerciseType)
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

    // 녹화 상태에 따른 폴리라인 관리 함수
    private fun observeRecordingState() {
        recordViewModel.isRecording.observe(viewLifecycleOwner, Observer { isRecording ->
            if (isExerciseMap && isRecording) {
                setupPolyline()
            } else {
                hidePolyline()
            }
        })
    }

    // 폴리라인 제거 함수
    private fun hidePolyline() {
        if (this::polyline.isInitialized) {
            polyline.remove()
        }
    }

    private fun shouldAddPoint(latLng: LatLng): Boolean {
        return true
    }

    // Firebase에서 GPX 파일 로드 함수
    private fun loadGpxFromFirebase() {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("gpxfiles")

        storageRef.listAll().addOnSuccessListener { listResult ->
            for (item in listResult.items) {
                if (item.name.endsWith(".gpx")) {
                    item.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                        val inputStream = bytes.inputStream()
                        val gpxParser = GpxParser()
                        val (coordinates, elevationData) = gpxParser.parse(inputStream)
                        drawPolylineOnMap(coordinates, "GPX Route", elevationData.first, elevationData.second, null)
                    }.addOnFailureListener { exception ->
                        Log.e("MapFragment", "Failed to load GPX file: ${exception.message}")
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("MapFragment", "Failed to list GPX files: ${exception.message}")
        }
    }

    private fun getExerciseTypeFromPreferences(): String {
        val sharedPreferences = context?.getSharedPreferences("activityPickSave", Context.MODE_PRIVATE)
        return sharedPreferences?.getString("selected_icon", "running") ?: "running"
    }

    private fun searchPolylineAndMoveCamera(query: String){
        val matchedPolyline = polylineList.firstOrNull{ it.first.contains(query, ignoreCase = true)}
        if (matchedPolyline != null) {
            // 폴리라인의 첫 번째 좌표로 카메라 이동
            val coordinates = matchedPolyline.second.points
            if (coordinates.isNotEmpty()) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates.first(), 16f))
            }
        } else {
            Log.e("MapFragment", "No matching polyline found for $query")
        }
    }

    override fun onPause() {
        super.onPause()
        // 위치 업데이트 중단
        if (isRequestingLocationUpdates) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isRequestingLocationUpdates = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 지도 자원 해제
        if (this::mMap.isInitialized) {
            mMap.clear()
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment != null) {
            childFragmentManager.beginTransaction().remove(mapFragment).commitAllowingStateLoss()
        }
    }

    companion object {
        // newInstance 메서드로 isExerciseMap 값을 전달
        fun newInstance(isExerciseMap: Boolean): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putBoolean("IS_EXERCISE_MAP", isExerciseMap)
            fragment.arguments = args
            return fragment
        }

        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
