package com.kisayo.sspurt.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
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
    private val REQUEST_CODE_LOCATION_PERMISSION = 1001



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //헬스커넥트 초기화
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        // 위치 권한 요청
//        checkLocationPermissionAndStartService()

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
//
//    private fun checkLocationPermissionAndStartService() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            // 권한이 없으면 권한 요청
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
//                ),
//                REQUEST_CODE_LOCATION_PERMISSION
//            )
//        } else {
//            // 권한이 이미 있을 경우 서비스 시작
//            startLocationService()
//        }
//    }
//
//    private fun startLocationService() {
//        val serviceIntent = Intent(this, TrackingService::class.java)
//        startForegroundService(serviceIntent) // API 26 이상에서는 startForegroundService 필요
//    }
//
//    // 권한 요청에 대한 응답 처리
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.isNotEmpty() &&
//            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//            // 권한이 승인된 경우 서비스 시작
//            startLocationService()
//        } else {
//            // 권한이 거부된 경우
//            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
//        }
//    }
}

