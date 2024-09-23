package com.kisayo.sspurt.activities.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.kisayo.sspurt.G
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.data.NidUserInfoResponse
import com.kisayo.sspurt.data.UserAccount
import com.kisayo.sspurt.databinding.ActivityLoginBinding
import com.kisayo.sspurt.networks.RetrofitHelper
import com.kisayo.sspurt.networks.RetrofitService
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = Firebase.auth
        sharedPreferences = getSharedPreferences("LoginInfo", MODE_PRIVATE)

        //id저장 체크박스 활성화
        binding.autoIdCb.isChecked = sharedPreferences.getBoolean("autoIdCb", false)
        if (binding.autoIdCb.isChecked) {
            binding.usernameSignUpEt.setText(sharedPreferences.getString("savedEmail", ""))
        }

        //자동로그인 체크박스 활성화
        if (sharedPreferences.getBoolean("AutoLoginChecked", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //로그인 버튼 클릭
        binding.loginIv.setOnClickListener { clickLogin() }

        //email 가입 버튼 클릭
        binding.createEmailAcountTv.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }

        //로그인 api 연동 버튼 처리
        binding.kakaoLogin.setOnClickListener { clickKakako() }
        binding.naverLogin.setOnClickListener { clickNaver() }
        binding.googleLogin.setOnClickListener { clickGoogle() }


//        binding.autologinCb.setOnCheckedChangeListener{}

        //카카오에서 사용하는 keyHash 인증서지문 값 얻어오기
        val keyHash: String = Utility.getKeyHash(this)
        Log.i("keyHash", keyHash)

        //firestore 초기화
        FirebaseApp.initializeApp(this)


    }// oncreate....

    private fun clickKakako() {
        //kakao login api library 추가하기 - kakao developer
        //카카오톡으로 로그인 & 카카오계정으로 로그인

        //카카오로그인 공통으로 사용하는 콜백 구성
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Toast.makeText(this, "카카오 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "카카오 로그인에 성공했습니다.", Toast.LENGTH_SHORT).show()
                //사용자 정보요청
                UserApiClient.instance.me { user, error ->
                    if (user != null) {
                        val id: String = user.id.toString()
                        val email: String = user.kakaoAccount?.email ?: ""
                        //val nickName: String = user?.kakaoAccount?.profile?.nickname ?: ""
                        //val profileImg: String = user?.kakaoAccount?.profile?.profileImageUrl ?: ""
                        Toast.makeText(this, "$email", Toast.LENGTH_SHORT).show()
                        G.userAccount = UserAccount(id, email, "kakao")
                        //main화면 이동
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }

    }//clickKokao

    private fun clickNaver() {
        NaverIdLoginSDK.initialize(this, "wDo9T2NcQZeYqo9NBTdn", "ubUixOlNqV", "Sspurt")
        NaverIdLoginSDK.authenticate(this, object : OAuthLoginCallback {
            override fun onError(errorCode: Int, message: String) {
                Toast.makeText(this@LoginActivity, "네이버 로그인 에러 : $message", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Toast.makeText(this@LoginActivity, "네이버 로그인 실패 : $message", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onSuccess() {
                Toast.makeText(this@LoginActivity, "네이버 로그인 성공", Toast.LENGTH_SHORT).show()

                //회원 profile 정보는 REST API를 이용하여 요청하고 응답받아야함.  - API 사용 명세서 확인
                //회원정보를 얻어오기 위한 토큰
                val accessToken: String? = NaverIdLoginSDK.getAccessToken()
                Log.i("token", "$accessToken")

                //Retrfit
                val retrofit = RetrofitHelper.getRetofitInstance("https://openapi.naver.com/")
                val retrofitService = retrofit.create(RetrofitService::class.java)
                val call = retrofitService.getNidUserInfo("Bearer $accessToken")
                call.enqueue(object : Callback<NidUserInfoResponse> {
                    override fun onResponse(
                        p0: Call<NidUserInfoResponse>, p1: Response<NidUserInfoResponse>
                    ) {
                        val userInfo = p1.body()
                        val id: String = userInfo?.response?.id ?: ""
                        val email: String = userInfo?.response?.email ?: ""

                        Toast.makeText(this@LoginActivity, "$email", Toast.LENGTH_SHORT).show()
                        G.userAccount = UserAccount(id, email, "naver")

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }

                    override fun onFailure(p0: Call<NidUserInfoResponse>, p1: Throwable) {
                        Toast.makeText(
                            this@LoginActivity, "회원정보 불러오기 실패 : ${p1.message}", Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        })


    }// clickNaver

    private fun clickGoogle() {

    }

    private fun clickLogin() {
        val email = binding.usernameSignUpEt.text.toString()
        val password = binding.passwordSignUpEt.text.toString()

        // 이메일이나 비밀번호가 비어있으면 toast
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        //firebase authentication 로그인 시도
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                //체크박스 상태에 따라 sharedpreferencesㅇ에 저장
                val editor = sharedPreferences.edit()
                if (binding.autoIdCb.isChecked) {
                    editor.putString("savedEmail", email) // 이메일 저장
                    editor.putBoolean("autoIdCb", true) // 아이디 저장 체크 상태
                } else {
                    editor.remove("savedEmail") //이메일 제거
                    editor.putBoolean("autoIdCb", false) // 아이디 저장 체크 해제
                }
                if(binding.autologinCb.isChecked){
                    editor.putBoolean("AutoLoginChecked",true)
                } else{
                    editor.putBoolean("AutoLoginChecked", false)
                }
                editor.apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "로그인 실패 : ${task.exception?.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}