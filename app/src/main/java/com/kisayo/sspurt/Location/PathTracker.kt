package com.kisayo.sspurt.Location

import com.google.android.gms.maps.model.LatLng // 위도 경도 모델
import com.google.firebase.firestore.FirebaseFirestore // Firestore 클래스
import android.util.Log // 로그 클래스

class PathTracker(private val firestore: FirebaseFirestore) {
    private val pathPoints: MutableList<LatLng> = mutableListOf() // 경로 포인트 리스트

    fun addPoint(point: LatLng) {
        pathPoints.add(point) // 포인트 추가
        savePathToFirestore() // Firestore에 경로 저장
    }

    fun getPath(): List<LatLng> {
        return pathPoints // 경로 반환
    }

    private fun savePathToFirestore() {
        // 경로 데이터를 Firestore에 저장
        val pathData = mapOf(
            "points" to pathPoints.map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
        )
        firestore.collection("paths") // "paths" 컬렉션에 저장
            .add(pathData)
            .addOnSuccessListener { Log.d("Firestore", "Path saved successfully") } // 성공 로그
            .addOnFailureListener { e -> Log.w("Firestore", "Error saving path", e) } // 실패 로그
    }
}