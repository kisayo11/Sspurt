package com.kisayo.sspurt.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kisayo.sspurt.Adapter.LookupFragmentAdapter
import com.kisayo.sspurt.activities.TrackingSaveActivity
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        firestore.collectionGroup("exerciseData")
            .whereEqualTo("isShared", true)  // isShared가 true인 데이터만 가져옴
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(14)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("LookUpFragment", "Exercise records fetched successfully.")
                Log.d("LookUpFragment", "Documents fetched: ${documents.size()}") // 가져온 문서 수 로그

                for (document in documents) {
                    Log.d("LookUpFragment", "Fetched Document: ${document.data}") // 각 문서 데이터 출력
                    val exerciseRecord = document.toObject(ExerciseRecord::class.java)
                    exerciseRecords.add(exerciseRecord)
                }
                isExerciseRecordsLoaded = true
                Log.d("LookUpFragment", "Exercise records loaded: ${exerciseRecords.size}")
                checkDataAndCombine() // 두 데이터 로드 확인
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting exercise records: ", exception)
            }
    }

    private fun fetchUserAccounts() {
        firestore.collection("account").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val userAccount = document.toObject(UserAccount::class.java)
                userAccounts.add(userAccount)
            }
            isUserAccountsLoaded = true
            checkDataAndCombine() // 두 데이터 로드 확인
        }.addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting user accounts: ", exception)
        }
    }

    // 두 데이터가 모두 로드되었는지 확인 후 결합 수행
    private fun checkDataAndCombine() {
        if (isExerciseRecordsLoaded && isUserAccountsLoaded) {
            combineRecords()
        } else {
        }
    }

    private fun combineRecords() {
        combinedRecords.clear() // 기존 데이터를 지운다
        Log.d("LookUpFragment", "Combining records...")

        for (exerciseRecord in exerciseRecords) {
            val userAccount = userAccounts.find { it.email == exerciseRecord.ownerEmail }
            if (userAccount != null) {
                combinedRecords.add(CombinedRecord(exerciseRecord, userAccount))
            }
        }

        Log.d("LookUpFragment", "Combined records count: ${combinedRecords.size}")

        // 어댑터 업데이트
        updateAdapter(combinedRecords) // combinedRecords를 전달
    }

    private fun updateAdapter(combinedRecords: List<CombinedRecord>) {
        Log.d("LookUpFragment", "Updating adapter with combined records...")
        adapter =
            LookupFragmentAdapter(requireContext(), combinedRecords) { clickedExerciseRecordId ->
                val intent = Intent(requireContext(), TrackingSaveActivity::class.java).apply {
                    putExtra("exerciseRecordId", clickedExerciseRecordId) // 클릭된 exerciseRecordId 전달
                    putExtra("sourceFragment", "LookUp") // LookUpFragment에서 왔다는 식별자
                }
                Log.d("LookUpFragment", "Starting TrackingSaveActivity with exerciseRecordId: $clickedExerciseRecordId")
                startActivity(intent)
            }

        binding.recyclerViewLookup.adapter = adapter
        Log.d("LookUpFragment", "Adapter updated.")
    }
}