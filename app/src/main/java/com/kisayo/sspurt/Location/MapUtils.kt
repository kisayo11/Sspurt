package com.kisayo.sspurt.Location

import com.google.android.gms.maps.model.LatLng // 위도 경도 모델
import com.google.android.gms.maps.model.PolylineOptions // Polyline 옵션 클래스
import android.graphics.Color // 색상 클래스

object MapUtils {
    fun createPolylineOptions(path: List<LatLng>): PolylineOptions {
        // Polyline 옵션 생성
        return PolylineOptions()
            .color(Color.BLUE) // 선 색상
            .width(10f) // 선 두께
            .addAll(path) // 경로 추가
    }
}