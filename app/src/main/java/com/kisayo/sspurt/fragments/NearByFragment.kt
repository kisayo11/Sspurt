package com.kisayo.sspurt.fragments

import android.os.Bundle // Bundle 클래스
import android.view.LayoutInflater // 레이아웃 인플레이터
import android.view.View // View 클래스
import android.view.ViewGroup // ViewGroup 클래스
import androidx.fragment.app.Fragment // Fragment 클래스
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.FragmentNearByBinding


class NearByFragment : Fragment() {
    private var _binding: FragmentNearByBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNearByBinding.inflate(inflater, container, false)
        val view = binding.root

        // `MapFragment`를 폴리라인 없이 `nearby` 모드로 사용
        val nearbyMapFragment = MapFragment.newInstance(false)

        childFragmentManager.beginTransaction()
            .replace(R.id.map_fragment_container, nearbyMapFragment)
            .commit()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
