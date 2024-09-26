package com.kisayo.sspurt.fragments

import android.os.Bundle // Bundle 클래스
import android.view.LayoutInflater // 레이아웃 인플레이터
import android.view.View // View 클래스
import android.view.ViewGroup // ViewGroup 클래스
import androidx.fragment.app.Fragment // Fragment 클래스
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

        // MapFragment를 동적으로 추가
        val mapFragment = MapFragment()
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
