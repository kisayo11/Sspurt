package com.kisayo.sspurt.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle // Bundle 클래스
import android.view.LayoutInflater // 레이아웃 인플레이터
import android.view.View // View 클래스
import android.view.ViewGroup // ViewGroup 클래스
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment // Fragment 클래스
import com.google.android.gms.maps.CameraUpdateFactory // 카메라 업데이트 팩토리
import com.google.android.gms.maps.GoogleMap // 구글 맵 객체
import com.google.android.gms.maps.OnMapReadyCallback // 맵 준비 콜백
import com.google.android.gms.maps.SupportMapFragment // 서포트 맵 프래그먼트
import com.google.android.gms.maps.model.LatLng // 위도 경도 모델
import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.Location.LocationManager
import com.kisayo.sspurt.Location.MapUtils
import com.kisayo.sspurt.Location.PathTracker
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.FragmentNearByBinding


class NearByFragment : Fragment() {

    private lateinit var binding: FragmentNearByBinding // View Binding
    private lateinit var map: GoogleMap // 구글 맵 객체
    private lateinit var locationManager: LocationManager // 위치 관리 객체
    private lateinit var pathTracker: PathTracker // 경로 추적 객체
    private val firestore = FirebaseFirestore.getInstance() // Firestore 인스턴스
    private val LOCATION_PERMISSION_REQUEST_CODE = 1 // 위치 권한 요청 코드


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNearByBinding.inflate(inflater, container, false) // 뷰 바인딩 초기화
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationManager = LocationManager(requireContext()) // 위치 관리자 초기화
        pathTracker = PathTracker(firestore) // 경로 추적기 초기화

        val mapFragment = childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap // 구글 맵 객체 할당
            setupMap() // 맵 설정
        }
    }

    private fun setupMap() {
        // 위치 권한 체크
        if (
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        map.isMyLocationEnabled = true // 현재 위치 표시 활성화
        getCurrentLocation() // 현재 위치 가져오기
    }

    private fun getCurrentLocation() {
        locationManager.getCurrentLocation { userLocation ->
            // 현재 위치로 카메라 이동
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            pathTracker.addPoint(userLocation) // 경로에 현재 위치 추가
            drawPath() // 경로 그리기
        }
    }

    private fun drawPath() {
        // 경로를 Polyline으로 그리기
        val polylineOptions = MapUtils.createPolylineOptions(pathTracker.getPath())
        map.addPolyline(polylineOptions) // 맵에 Polyline 추가
    }
}