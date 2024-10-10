package com.kisayo.sspurt.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kisayo.sspurt.fragments.TutorialFragment

class TutorialPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val tutorialPages = listOf(
        TutorialFragment.newInstance("환영합니다", "Sspurt의 주요 기능을 소개합니다!"),
        TutorialFragment.newInstance("위치 추적", "운동을 기록하기 위해 위치 권한을 허용해주세요."),
        TutorialFragment.newInstance("운동 시작", "이 버튼을 눌러 운동을 시작하세요."),
        TutorialFragment.newInstance("경로 저장", "경로를 저장하고 친구들과 공유할 수 있습니다.")
    )

    override fun getItemCount(): Int {
        return tutorialPages.size
    }

    override fun createFragment(position: Int): Fragment {
        return tutorialPages[position]
    }
}