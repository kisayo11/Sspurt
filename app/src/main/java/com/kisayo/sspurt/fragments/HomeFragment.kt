package com.kisayo.sspurt.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kisayo.sspurt.Adapter.HomeFragmentAdapter
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.TrackingStartActivity
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.databinding.FragmentHomeBinding
import com.kisayo.sspurt.utils.UserRepository

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HomeFragmentAdapter
    private var exerciseRecords : MutableList<ExerciseRecord> = mutableListOf() // 데이터리스트 초기화
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: UserRepository


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater,container,false)

        //초기화즈...
        firestore = FirebaseFirestore.getInstance()
        repository = UserRepository(requireContext())
        binding.recyclerViewList.layoutManager = LinearLayoutManager(context)

        fetchExerciseRecords()

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // 플로팅버튼 "Start" 클릭리스너
        binding.startFab.setOnClickListener {
            val intent = Intent(requireContext(), TrackingStartActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchExerciseRecords(){
        exerciseRecords.clear() // 리스트 초기화
        val Email = repository.getCurrentUserEmail()
        if(Email !=null){
            firestore.collection("account")
                .document(Email)
                .collection("exerciseData")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val exerciseRecord = document.toObject(ExerciseRecord::class.java)
                        exerciseRecords.add(exerciseRecord) // 리스트에 추가
                    }
                    // 예시: HomeFragment에서 어댑터를 설정할 때
                    adapter = HomeFragmentAdapter(requireContext(), exerciseRecords) { clickedEmail, clickedDate ->
                        val fragment = RecordDataFragment().apply {
                            arguments = Bundle().apply {
                                putString("dataType", "mydata")
                                putString("email", clickedEmail)
                                putString("date", clickedDate)
                            }
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null) // 뒤로 가기 가능하게 설정
                            .commit()
                    }
                    binding.recyclerViewList.adapter = adapter // RecyclerView에 어댑터 설정
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        } else {
            Log.e("Auth", "User is not logged in.")
        }
    }


}
