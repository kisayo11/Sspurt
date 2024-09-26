package com.kisayo.sspurt.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.GpsConfirmActivity

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 현재 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청 필요
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                GpsConfirmActivity.LOCATION_PERMISSION_REQUEST_CODE) // GPSActivity의 상수 사용
            return
        }

        mMap.isMyLocationEnabled = true

        // 현재 위치 요청
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // 현재 위치를 LatLng으로 변환
                val currentLocation = LatLng(location.latitude, location.longitude)
                // 카메라를 현재 위치로 이동
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f)) // 줌 레벨 설정
            }
        }
    }
}