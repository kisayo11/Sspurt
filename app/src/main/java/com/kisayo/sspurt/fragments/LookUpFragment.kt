package com.kisayo.sspurt.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kisayo.sspurt.Adapter.HomeFragmentAdapter
import com.kisayo.sspurt.Adapter.LookupFragmentAdapter
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.data.UserAccount
import com.kisayo.sspurt.databinding.FragmentLookUpBinding
import com.kisayo.sspurt.utils.UserRepository
import kotlin.Pair

class LookUpFragment : Fragment(){

    private lateinit var binding: FragmentLookUpBinding
    private lateinit var adapter: LookupFragmentAdapter
    private var exerciseRecords : MutableList<ExerciseRecord> = mutableListOf()
    private var userAccounts : MutableList<UserAccount> = mutableListOf()
    private var combinedRecords: MutableList<Pair<ExerciseRecord, UserAccount>> = mutableListOf() // 운동 데이터와 사용자 정보를 쌍으로 저장
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentLookUpBinding.inflate(inflater,container,false)

        //초기화즈...
        firestore = FirebaseFirestore.getInstance()
        repository = UserRepository(requireContext())
        binding.recyclerViewLookup.layoutManager = GridLayoutManager(requireContext(),2)


        fetchExerciseRecords()         // 운동 기록과 사용자 정보를 가져오기
        fetchUserAccounts() // 사용자 계정 정보 가져오기

        return binding.root

    }
    private fun fetchExerciseRecords() {
        val email = repository.getCurrentUserEmail()
        if (email != null) {
            firestore.collection("account")
                .document(email)
                .collection("exerciseData")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val exerciseRecord = document.toObject(ExerciseRecord::class.java)
                        exerciseRecords.add(exerciseRecord) // 운동 기록 리스트에 추가
                    }
                    combineRecords() // 운동 기록과 사용자 계정을 결합
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting documents: ", exception)
                }
        } else {
            Log.e("Auth", "User is not logged in.")
        }
    }

    private fun fetchUserAccounts() {
        firestore.collection("account")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userAccount = document.toObject(UserAccount::class.java)
                    userAccounts.add(userAccount) // 사용자 계정 리스트에 추가
                }
                combineRecords() // 사용자 계정 정보를 가져온 후 결합
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting user accounts: ", exception)
            }
    }

    // 운동 기록과 사용자 계정을 결합하는 함수
    private fun combineRecords() {
        // 각 운동 기록에 대해 해당하는 사용자 계정과 결합
        exerciseRecords.forEach { exerciseRecord ->
            // 사용자 계정 중에서 이메일이 일치하는 것을 찾아 결합
            userAccounts.find { it.email == repository.getCurrentUserEmail() }?.let { userAccount ->
                combinedRecords.add(Pair(exerciseRecord, userAccount)) // 운동 기록과 사용자 계정의 쌍 추가
            }
        }
        updateAdapter() // 어댑터 업데이트
    }

    private fun updateAdapter() {
        adapter = LookupFragmentAdapter(requireContext(), combinedRecords) // 결합된 레코드로 어댑터 초기화
        binding.recyclerViewLookup.adapter = adapter // RecyclerView에 어댑터 설정
    }
}