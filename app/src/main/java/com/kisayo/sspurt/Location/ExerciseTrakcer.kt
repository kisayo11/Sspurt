package com.kisayo.sspurt.Location

import android.location.Location
import com.kisayo.sspurt.data.ExerciseRecord

class ExerciseTracker {
    private var startLocation: Location? = null // 시작 위치
    private var previousLocation: Location? = null // 이전 위치
    private var totalDistance: Double = 0.0 // 총 이동 거리
    private var totalTime: Long = 0 // 총 시간
    private var previousTime: Long = 0 // 이전 시간
    var currentSpeed: Double = 0.0 // 현재 속도를 클래스 변수로 선언
    private var elapsedTime: Long = 0 // 경과 시간
    private var exerciseData = ExerciseRecord() // ExerciseRecord 객체 초기화



    // 사용자 체중 및 MET 값 기본값
    private var userWeight: Double = 70.0 // 기본 체중 (kg)
    private var metValue: Double = 8.0 // 기본 MET 값 (예시: 격렬한 운동)

    // 운동 시작 시 호출
    fun startTracking(location: Location) {
        startLocation = location // 시작 위치 저장
    }

    // 현재 위치에서 속도를 계산하는 메서드
    fun calculateCurrentSpeed(currentLocation: Location): Double {
        val speed = currentLocation.speed // GPS에서 제공하는 속도
        previousLocation = currentLocation // 이전 위치 업데이트
        return speed.toDouble() // m/s로 반환
    }

    fun updateDistance(currentLocation: Location?) {
        previousLocation?.let {
            val distance = it.distanceTo(currentLocation!!) // 두 위치 간의 거리 (미터)
            totalDistance += distance // 총 이동 거리 업데이트
            exerciseData.distance += distance // exerciseData의 이동 거리도 업데이트
        }
        previousLocation = currentLocation // 현재 위치를 이전 위치로 업데이트
    }

    // 평균 속도 계산
    fun calculateAverageSpeed(): Double {
        return if (elapsedTime > 0) {
            (totalDistance / (elapsedTime / 3600.0)) // 평균 속도 (km/h) 계산
        } else {
            0.0
        }
    }

    // 칼로리 계산
    fun calculateCalories(elapsedTime: Long): Double {
        return (metValue * userWeight * (elapsedTime / 3600.0)) // 칼로리 계산
    }

    // 심장 강화 점수 계산
    fun calculateHeartHealthScore(
        averageSpeed: Double, // 평균 속도 (km/h)
        duration: Long, // 운동 시간 (초 단위)
        caloriesBurned: Double // 소모 칼로리
    ): Int {
        var score = 0

        // 평균 속도 기준 점수
        score += when {
            averageSpeed < 3.0 -> 1 // 느림
            averageSpeed in 3.0..5.0 -> 2 // 보통
            averageSpeed > 5.0 -> 3 // 빠름
            else -> 0
        }

        // 운동 시간 기준 점수
        score += when {
            duration < 1800 -> 1 // 30분 미만
            duration in 1800..3600 -> 2 // 30분에서 1시간
            duration > 3600 -> 3 // 1시간 초과
            else -> 0
        }

        // 칼로리 소모 기준 점수
        score += when {
            caloriesBurned < 200 -> 1 // 200칼로리 미만
            caloriesBurned in 200.0..500.0 -> 2 // 200에서 500칼로리
            caloriesBurned > 500 -> 3 // 500칼로리 초과
            else -> 0
        }

        // 점수 조정 (최대 5점)
        return minOf(score, 5)
    }

    // 이동 시간 포맷팅
    fun formatElapsedTime(elapsedTime: Long): String {
        val seconds = elapsedTime % 60
        val minutes = (elapsedTime / 60) % 60
        val hours = elapsedTime / 3600
        return String.format("%02d:%02d:%02d", hours, minutes, seconds) // HH:mm:ss 포맷
    }

    // 사용자 체중 및 MET 값 설정
    fun setUserWeight(weight: Double) {
        userWeight = weight
    }

    fun setMetValue(met: Double) {
        metValue = met
    }

    // 시간 업데이트
    fun updateTime(currentTime: Long) {
        if (previousTime != 0L) {
            totalTime += currentTime - previousTime // 총 시간 계산
        }
        previousTime = currentTime // 이전 시간 업데이트
    }

    fun getTotalDistance(): Double {
        return totalDistance
    }
    fun getTotalTime() = totalTime
}