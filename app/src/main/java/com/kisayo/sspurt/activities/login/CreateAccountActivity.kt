package com.kisayo.sspurt.activities.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.Constants
import com.kisayo.sspurt.databinding.ActivityCreateAcountBinding

class CreateAccountActivity : AppCompatActivity() {

    val binding by lazy { ActivityCreateAcountBinding.inflate(layoutInflater) }
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()


        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.signUpPushIv.setOnClickListener { clickSignUp() }

    }

    private fun clickSignUp() {
        val email = binding.emailSignUpEt.text.toString()
        val password = binding.passwordSignUpEt.text.toString()
        val passwordConfirm = binding.password2SignUpEt.text.toString()
        val username = binding.usernameSignUpEt.text.toString()
        val mobile = binding.mobileSignUpEt.text.toString()

        // 비밀번호 확인
        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            binding.password2SignUpEt.selectAll()
            return
        }

        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods != null && signInMethods.isNotEmpty()) {
                        Toast.makeText(this, "동일한 이메일이 이미 사용 중입니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        // 이메일 사용이 가능하므로 회원가입 진행
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { signUpTask ->
                                if (signUpTask.isSuccessful) {

                                    val user = auth.currentUser
                                    user?.let {
                                        val userInfo = hashMapOf(
                                            "email" to email,
                                            "username" to username,
                                            "mobile" to mobile,
                                            "profileImageUrl" to ""
                                        )

                                        firestore.collection(Constants.ACCOUNT_COLLECTION)
                                            .document(email)
                                            .set(userInfo)
                                            .addOnCompleteListener { saveTask ->
                                                if (saveTask.isSuccessful) {
                                                    AlertDialog.Builder(this)
                                                        .setMessage("회원가입이 완료되었습니다.")
                                                        .setPositiveButton("확인") { dialog, _ ->
                                                            dialog.dismiss()
                                                            startActivity(Intent( this, LoginActivity::class.java))
                                                            finish()
                                                        }.setCancelable(false).create().show()
                                                } else{
                                                    Toast.makeText(this, "사용자 정보 저장 오류", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                                } else{
                                    Toast.makeText(this, "회원가입 오류 \n동일한 이메일이 이미 사용중입니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            }
    }
}
