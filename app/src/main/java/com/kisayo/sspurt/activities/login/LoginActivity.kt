package com.kisayo.sspurt.activities.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        //로그인 버튼 클릭
        binding.loginIv.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        //가입후 초기화면으로 돌아올수 있게 finish()를 하지 않을 것
        binding.createAcountTv.setOnClickListener {
            startActivity(Intent(this,CreateAcountActivity::class.java))
        }

    }
}