package com.kisayo.sspurt.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.ActivityMainBinding
import com.kisayo.sspurt.fragments.HomeFragment
import com.kisayo.sspurt.fragments.LookUpFragment
import com.kisayo.sspurt.fragments.MyAccountFragment
import com.kisayo.sspurt.fragments.NearByFragment

class MainActivity : AppCompatActivity() {

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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




        // 플로팅버튼 "Start" 클릭리스너
        binding.startFab.setOnClickListener {
            startActivity(Intent(this,TrackingStartActivity::class.java))
        }


        }
}
