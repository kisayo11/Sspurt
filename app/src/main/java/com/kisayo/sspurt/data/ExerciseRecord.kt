package com.kisayo.sspurt.data

import android.location.Location
import com.google.android.gms.maps.model.LatLng

data class ExerciseRecord(
    var isRecording: Boolean = false, // 기록 중인지 여부
    var isPaused: Boolean = false, // 일시 중지 상태
    var elapsedTime: Long = 0, // 경과 시간
    var distance: Double = 0.0, // 이동 거리
    var currentSpeed: Double = 0.0, // 현재 속도
    var averageSpeed: Double = 0.0, // 평균 속도
    var heartHealthScore: Int = 0, // 심장 강화 점수
    var calories: Double = 0.0, // 칼로리
    var temperature: Double = 0.0, // 기온
    var exerciseType: String = "", // 운동 종류
    var userFeedback: String = "", // 사용자 피드백
    var currentLocation: LatLng? = null, // 현재 위치 정보
    var date: String = System.currentTimeMillis().toString(), // 날짜
    var photoUrl: String? = null, // 사진 URL (옵션)
    var exerciseJournal: String? = null // 운동 일지 (옵션)
)
