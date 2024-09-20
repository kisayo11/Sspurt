package com.kisayo.sspurt.activities.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.kisayo.sspurt.G
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.data.UserAccount
import com.kisayo.sspurt.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)




        //로그인 버튼 클릭
        binding.loginIv.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //email 가입 버튼 클릭
        binding.createEmailAcountTv.setOnClickListener {
            startActivity(Intent(this,CreateAccountActivity::class.java))
        }

        //로그인 api 연동 버튼 처리
        binding.kakaoLogin.setOnClickListener { clickKakako() }
        binding.naverLogin.setOnClickListener { clickNaver() }
        binding.googleLogin.setOnClickListener { clickGoogle() }

        //카카오에서 사용하는 keyHash 인증서지문 값 얻어오기
        val keyHash: String = Utility.getKeyHash(this)
        Log.i("keyHash", keyHash)


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
                    if (error != null) {
                        val id: String = user?.id.toString()
                        val email: String = user?.kakaoAccount?.email ?: ""
                        val nickName: String = user?.kakaoAccount?.profile?.nickname ?: ""
                        val profileImg: String = user?.kakaoAccount?.profile?.profileImageUrl ?: ""
                        Toast.makeText(this, "$email \n $nickName", Toast.LENGTH_SHORT).show()
                        G.userAccount = UserAccount(id, email, "kakao")
                        //main화면 이동
                        startActivity(Intent(this, MainActivity::class.java))
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
    private fun clickNaver() {}
    private fun clickGoogle() {}





}