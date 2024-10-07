package com.kisayo.sspurt.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kisayo.sspurt.Adapter.LookupFragmentAdapter
import com.kisayo.sspurt.R
import com.kisayo.sspurt.data.CombinedRecord
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.data.UserAccount
import com.kisayo.sspurt.databinding.FragmentLookUpBinding

class LookUpFragment : Fragment() {

    private lateinit var binding: FragmentLookUpBinding
    private lateinit var adapter: LookupFragmentAdapter
    private var exerciseRecords: MutableList<ExerciseRecord> = mutableListOf()
    private var userAccounts: MutableList<UserAccount> = mutableListOf()
    private var combinedRecords: MutableList<CombinedRecord> = mutableListOf()
    private lateinit var firestore: FirebaseFirestore
    private var isExerciseRecordsLoaded = false
    private var isUserAccountsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLookUpBinding.inflate(inflater, container, false)

        // Firestore 초기화
        firestore = FirebaseFirestore.getInstance()
        binding.recyclerViewLookup.layoutManager = GridLayoutManager(requireContext(), 2)

        // 데이터 가져오기 시작
        fetchExerciseRecords()
        fetchUserAccounts()

        return binding.root
    }

    private fun fetchExerciseRecords() {
        exerciseRecords.clear() // 리스트 초기화
        firestore.collectionGroup("exerciseData")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val exerciseRecord = document.toObject(ExerciseRecord::class.java)
                    exerciseRecords.add(exerciseRecord)
                }
                isExerciseRecordsLoaded = true
                checkDataAndCombine() // 두 데이터 로드 확인
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting exercise records: ", exception)
            }
    }

    private fun fetchUserAccounts() {
        firestore.collection("account")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userAccount = document.toObject(UserAccount::class.java)
                    userAccounts.add(userAccount)
                }
                isUserAccountsLoaded = true
                checkDataAndCombine() // 두 데이터 로드 확인
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting user accounts: ", exception)
            }
    }

    // 두 데이터가 모두 로드되었는지 확인 후 결합 수행
    private fun checkDataAndCombine() {
        if (isExerciseRecordsLoaded && isUserAccountsLoaded) {
            combineRecords()
        }
    }

    private fun combineRecords() {
        combinedRecords.clear() // 기존 데이터를 지운다

        for (exerciseRecord in exerciseRecords) {
            val userAccount = userAccounts.find { it.email == exerciseRecord.ownerEmail }
            if (userAccount != null) {
                combinedRecords.add(CombinedRecord(exerciseRecord, userAccount))
            }
        }

        // combinedRecords가 비어 있는지 확인
        if (combinedRecords.isEmpty()) {
            // UI에 데이터가 없음을 표시 (예: Toast 메시지)
        }

        // 어댑터 업데이트
        updateAdapter(combinedRecords) // combinedRecords를 전달
    }

    private fun updateAdapter(combinedRecords: List<CombinedRecord>) {
        // CombinedRecord를 어댑터에 전달 및 아이템 클릭 리스너 설정
        adapter = LookupFragmentAdapter(requireContext(), combinedRecords) { clickedEmail, clickedDate ->
            val fragment = RecordDataFragment().apply {
                arguments = Bundle().apply {
                    putString("dataType", "shareddata")
                    putString("email", clickedEmail)
                    putString("date", clickedDate)
                }
            }
            // Fragment 전환
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // 백스택에 추가하여 뒤로 가기 가능
                .commit()
        }
        binding.recyclerViewLookup.adapter = adapter
    }
}