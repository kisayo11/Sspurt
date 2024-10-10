package com.kisayo.sspurt.activities.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import com.kisayo.sspurt.Adapter.TutorialPagerAdapter
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.databinding.ActivityTutorialBinding

class TutorialActivity : AppCompatActivity() {

    private val binding by lazy { ActivityTutorialBinding.inflate(layoutInflater) }
    private lateinit var pagerAdapter: TutorialPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        pagerAdapter = TutorialPagerAdapter(this)
        binding.tutorialViewPager.adapter = pagerAdapter

        binding.nextBtn.setOnClickListener {
            if(binding.tutorialViewPager.currentItem<pagerAdapter.itemCount -1){
                binding.tutorialViewPager.currentItem +=1
            }else{
                finishTutorial()
            }
        }
    }

    private fun finishTutorial(){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}