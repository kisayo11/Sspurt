package com.kisayo.sspurt.data


import com.google.firebase.Timestamp


data class ExerciseRecord(
    var exerciseRecordId: String = "", //운동데이터 고유식별자 ID
    var isRecording: Boolean = false, // 기록 중인지 여부
    var isPaused: Boolean = false, // 일시 중지 상태
    var elapsedTime: Long = 0, // 경과 시간
    var distance: Double = 0.0, // 이동 거리
    var currentSpeed: Double = 0.0, // 현재 속도
    var averageSpeed: Double = 0.0, // 평균 속도f
    var maxSpeed: Double = 0.0,
    var heartHealthScore: Int = 0, // 심장 강화 점수
    var calories: Double = 0.0, // 칼로리
    var temperature: Double = 0.0, // 기온
    var exerciseType: String = "", // 운동 종류
    var userFeedback: String = "", // 사용자 피드백
    var currentLocation: LatLngWrapper? = null, // 현재 위치 정보
    var date: Timestamp = Timestamp.now(), // 현재 날짜
    var photoUrl: String? = null, // 사진 URL (옵션)
    var exerciseJournal: String? = null, // 운동 일지 (옵션)
    var metValue: Double = 0.0, // MET 값 추가
    var realTimeData: RealTimeData? =null,
    var isShared: Boolean = false, // 공유 스위치
    var locationTag: String? = null, // 공유시 장소 태그 추가
    var routes: List<LatLngWrapper> = listOf(), // 경로 리스트 추가
    val ownerEmail: String = "", // 운동 기록 소유자의 이메일
    var capturePolylineOnly : String? = null, // 폴리라인만 캡쳐
    var captureMapWithPolyline : String? = null // 지도위 폴리라인 캡쳐

)

data class LatLngWrapper(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)

data class RealTimeData(
    var altitude: Double = 0.0,  // 고도
    var incline: Double = 0.0,   // 인클라인
    var decline: Double = 0.0     // 디클라인
)
