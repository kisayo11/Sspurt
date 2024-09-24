package com.kisayo.sspurt.Location

import android.Manifest // 권한 관련 클래스
import android.content.Context // 컨텍스트 클래스
import android.content.pm.PackageManager // 패키지 매니저 클래스
import androidx.core.app.ActivityCompat // 권한 관련 헬퍼
import com.google.android.gms.location.FusedLocationProviderClient // 위치 제공자 클라이언트
import com.google.android.gms.location.LocationServices // 위치 서비스
import com.google.android.gms.maps.model.LatLng // 위도 경도 모델
import android.location.Location // 위치 클래스

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context) // 위치 제공자 클라이언트

    fun getCurrentLocation(onSuccess: (LatLng) -> Unit) {
        // 위치 권한 체크
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청 필요
            return
        }

        // 마지막 위치 가져오기
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                onSuccess(LatLng(it.latitude, it.longitude)) // 위치 성공 콜백
            }
        }
    }
}