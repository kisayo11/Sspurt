package com.kisayo.sspurt.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kisayo.sspurt.R
import com.kisayo.sspurt.databinding.FragmentTutorialBinding


class TutorialFragment : Fragment() {

    private var _binding: FragmentTutorialBinding? = null
    private val binding get() = _binding!!

    private var title: String? = null
    private var description: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ViewBinding 인스턴스 생성
        _binding = FragmentTutorialBinding.inflate(inflater, container, false)

        title = arguments?.getString("title")
        description = arguments?.getString("description")

        // 바인딩을 통해 UI 업데이트
        binding.titleTextView.text = title
        binding.descriptionTextView.text = description

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수를 방지하기 위해 뷰 제거 시 바인딩 해제
    }

    companion object {
        fun newInstance(title: String, description: String): TutorialFragment {
            val fragment = TutorialFragment()
            val bundle = Bundle()
            bundle.putString("title", title)
            bundle.putString("description", description)
            fragment.arguments = bundle
            return fragment
        }
    }
}