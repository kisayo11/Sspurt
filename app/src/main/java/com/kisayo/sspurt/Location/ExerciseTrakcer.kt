package com.kisayo.sspurt.Location

import android.location.Location
import com.kisayo.sspurt.data.ExerciseRecord

class ExerciseTracker {
    // 사용자 체중 및 MET 값 기본값
    private var userWeight: Double = 70.0 // 기본 체중 (kg)
    private var metValue: Double = 8.0 // 기본 MET 값 (예시: 격렬한 운동)

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

    // 이동시간을 시간 단위로 계산
    fun elapsedTimeInHours(elapsedTime: Long): Double {
        return elapsedTime / 3600.0
    }




}