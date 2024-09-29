package com.kisayo.sspurt.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kisayo.sspurt.activities.TrackingSaveActivity
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.databinding.FragmentHealthRecordBinding
import com.kisayo.sspurt.utils.FirestoreHelper
import com.kisayo.sspurt.utils.UserRepository


class HealthRecordFragment : Fragment() {

    private lateinit var binding: FragmentHealthRecordBinding
    private var exerciseData = ExerciseRecord() // 통합된 운동 데이터
    private var recordingTimer: CountDownTimer? = null
    private val firestoreHelper = FirestoreHelper() // FirestoreHelper 인스턴스
    private lateinit var userRepository: UserRepository


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHealthRecordBinding.inflate(inflater, container, false)
        return binding.root
    }//onCreate...

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // 레코드 버튼(animationview) 클릭 리스너
        binding.recordAni.setOnClickListener {
            binding.countdownAni.visibility = View.VISIBLE
            binding.countdownAni.playAnimation()

            // 애니메이션 끝나면 GONE으로 설정
            binding.countdownAni.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.countdownAni.visibility = View.GONE
                    binding.recordAni.visibility = View.GONE

                    binding.pauseIb.visibility = View.VISIBLE
                    startRecording() // 레코딩 시작
                }
            })
        }

        //pause 버튼 클릭 리스너
        binding.pauseIb.setOnClickListener {
            if (binding.stopIb.visibility == View.VISIBLE) {
                binding.stopIb.visibility = View.INVISIBLE
            } else {
                binding.stopIb.visibility = View.VISIBLE

            }
            if (exerciseData.isPaused) {
                resumeRecording()
            } else {
                pauseRecording()
            }
        }


        //stop 버튼 클릭 리스너
        // 초기 색상 설정 (검은색 아이콘)
        binding.stopIb.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

        binding.stopIb.setOnLongClickListener {

            stopRecording() // 레코딩 중지

            // 애니메이션을 위한 ValueAnimator 설정
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 500 // 애니메이션 시간

            animator.addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedValue as Float
                val color = blendColors(Color.BLACK, Color.BLUE, fraction) // 색상 혼합
                binding.stopIb.setColorFilter(color, PorterDuff.Mode.SRC_IN) // 아이콘 색상 업데이트
            }

            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션이 끝난 후 액티비티로 이동
                    val intent = Intent(requireContext(), TrackingSaveActivity::class.java)
                    startActivity(intent)
                }
            })

            animator.start()
            true // 이벤트 소비

        }//stopIb click...

    }// onViewCreated

    private fun startRecording() {
        exerciseData.isRecording = true
        exerciseData.isPaused = false
        exerciseData.elapsedTime = 0 // 경과 시간 초기화
        startTimer() // 타이머 시작
    }

    private fun pauseRecording() {
        recordingTimer?.cancel() // 타이머 일시 정지
        exerciseData.isRecording = false
    }

    private fun resumeRecording() {
        exerciseData.isRecording = true
        exerciseData.isPaused = false
        startTimer() // 타이머 재개
    }

    private fun stopRecording() {
        try {
            recordingTimer?.cancel()
            exerciseData.isRecording = false
            saveExerciseData() // 데이터 저장 시도
        } catch (e: Exception) {
            Log.e("HealthRecordFragment", "Error in stopRecording: ${e.message}")
        }
    }

    private fun startTimer() {
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                exerciseData.elapsedTime++ // 경과 시간 증가
                exerciseData.currentSpeed = calculateCurrentSpeed() // 현재 속도 계산
                exerciseData.distance += exerciseData.currentSpeed // 이동 거리 업데이트

                // UI 업데이트
                binding.tv1.text = String.format("%.2f", exerciseData.distance) // 이동거리
                binding.tv2.text = String.format("%.2f", calculateAverageSpeed()) // 평균속도
                binding.tv3.text = formatElapsedTime(exerciseData.elapsedTime) // 이동시간
                binding.tv4.text = String.format("%.2f", exerciseData.currentSpeed) // 현재속도
                binding.tv5.text = exerciseData.heartHealthScore.toString() // 심장강화점수
                binding.tv6.text = String.format("%.2f", exerciseData.calories) // 칼로리
            }

            override fun onFinish() {
                // 타이머 종료 시 처리 (필요 시 구현)
            }
        }.start()
    }

    // 현재 속도를 계산하는 메서드 추가
    private fun calculateCurrentSpeed(): Double {
        // GPS나 센서 데이터를 사용해 현재 속도를 계산하는 로직
        return (Math.random() * 10) // 임시 값 (실제 구현 필요)
    }

    // 평균 속도를 계산하는 메서드 추가
    private fun calculateAverageSpeed(): Double {
        return if (exerciseData.elapsedTime > 0) {
            exerciseData.distance / (exerciseData.elapsedTime / 3600.0) // 평균 속도 계산
        } else {
            0.0
        }
    }

    // 경과 시간을 포맷팅하는 메서드 추가
    private fun formatElapsedTime(elapsedTime: Long): String {
        val seconds = elapsedTime % 60
        val minutes = (elapsedTime / 60) % 60
        val hours = elapsedTime / 3600
        return String.format("%02d:%02d:%02d", hours, minutes, seconds) // HH:mm:ss 포맷
    }

    private fun saveExerciseData() {
        // 현재 사용자의 이메일을 가져옵니다.
        val email = userRepository.getCurrentUserEmail() ?: return // 이메일 가져오기
        val exerciseRecord = ExerciseRecord(
            isRecording = exerciseData.isRecording,
            elapsedTime = exerciseData.elapsedTime,
            distance = exerciseData.distance,
            currentSpeed = exerciseData.currentSpeed,
            averageSpeed = exerciseData.averageSpeed,
            heartHealthScore = exerciseData.heartHealthScore,
            calories = exerciseData.calories,
            temperature = exerciseData.temperature,
            exerciseType = exerciseData.exerciseType,
            userFeedback = exerciseData.userFeedback,
            currentLocation = exerciseData.currentLocation,
            date = System.currentTimeMillis().toString(),
            photoUrl = exerciseData.photoUrl,
            exerciseJournal = exerciseData.exerciseJournal
        )

        // FirestoreHelper를 사용하여 운동 기록을 저장합니다.
        val firestoreHelper = FirestoreHelper()
        firestoreHelper.saveExerciseRecord(email, exerciseRecord, onSuccess = {
            Toast.makeText(requireContext(), "운동 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }, onFailure = { exception ->
            Toast.makeText(requireContext(), "저장 실패: ${exception.message}", Toast.LENGTH_SHORT)
                .show()
        })
    }


    // 색상 혼합 함수
    private fun blendColors(color1: Int, color2: Int, fraction: Float): Int {
        val alpha =
            (Color.alpha(color1) + fraction * (Color.alpha(color2) - Color.alpha(color1))).toInt()
        val red = (Color.red(color1) + fraction * (Color.red(color2) - Color.red(color1))).toInt()
        val green =
            (Color.green(color1) + fraction * (Color.green(color2) - Color.green(color1))).toInt()
        val blue =
            (Color.blue(color1) + fraction * (Color.blue(color2) - Color.blue(color1))).toInt()
        return Color.argb(alpha, red, green, blue)
    }
}


