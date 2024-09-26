package com.kisayo.sspurt.activities.preference

import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.databinding.ActivityBugReportBinding
import com.kisayo.sspurt.utils.UserRepository

class BugReportActivity : AppCompatActivity() {

    private val binding by lazy { ActivityBugReportBinding.inflate(layoutInflater) }
    private val database = FirebaseFirestore.getInstance()
    lateinit var userRepository : UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        userRepository = UserRepository(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.showId.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        val email = FirebaseAuth.getInstance().currentUser?.email
        binding.showId.text = email ?: ""

        binding.button.setOnClickListener { submitBugReport() }

    }

    private fun submitBugReport() {
        val email = userRepository.getCurrentUserEmail() // UserRepository에서 이메일 가져오기
        val reportText = binding.reportText.text.toString()
        val timestamp = System.currentTimeMillis() // 현재 시간

        if (email != null && reportText.isNotBlank()) {
            // Firestore에 리포트 저장
            val reportData = hashMapOf(
                "email" to email,
                "board" to reportText,
                "timestamp" to FieldValue.serverTimestamp()
            )

            database.collection("Bug Report")
                .add(reportData) // 자동으로 문서 ID 생성
                .addOnSuccessListener {
                    Toast.makeText(this, "리포트가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                    finish() // 액티비티 종료
                }
                .addOnFailureListener {
                    Toast.makeText(this, "리포트 전송 실패", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "유효한 리포트를 입력하세요.", Toast.LENGTH_SHORT).show()
        }
    }
}
