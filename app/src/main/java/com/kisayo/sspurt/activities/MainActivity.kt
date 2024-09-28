package com.kisayo.sspurt.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityMainBinding
import com.kisayo.sspurt.fragments.HomeFragment
import com.kisayo.sspurt.fragments.LookUpFragment
import com.kisayo.sspurt.fragments.MyAccountFragment
import com.kisayo.sspurt.fragments.NearByFragment

class MainActivity : AppCompatActivity() {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var healthConnectClient: HealthConnectClient
    private val REQUEST_CODE_BODY_SENSORS = 1 // 요청 코드 정의


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //헬스커넥트 초기화
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        //헬스커넥트 권한작업
        // 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                REQUEST_CODE_BODY_SENSORS)
        }
        else {
            // 권한이 이미 허용된 경우 헬스커넥트 데이터 읽기
            readHealthData()
        }


        //homefragment 설정
        supportFragmentManager.beginTransaction().add(R.id.fragment_container,HomeFragment()).commit()

        //bottomNavigationView 설정
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.nav_home->supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,HomeFragment()).commit()
                R.id.nav_nearby->supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,NearByFragment()).commit()
                R.id.nav_lookup->supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,LookUpFragment()).commit()
                R.id.nav_account->supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container,MyAccountFragment()).commit()
            }//when
            true
        }//setItemSelected
        }// oncreate

    private fun readHealthData(){

    }
}
