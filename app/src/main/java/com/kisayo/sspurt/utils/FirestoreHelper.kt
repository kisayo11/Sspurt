package com.kisayo.sspurt.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.data.RealTimeData

class FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    // 운동 기록 저장
    fun saveExerciseRecord(email: String, record: ExerciseRecord, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRecordRef = db.collection("account").document(email).collection("exerciseData").document()
        userRecordRef.set(record)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // 실시간 데이터 저장
    fun saveRealTimeData(email: String, realTimeData: RealTimeData, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val realTimeDataRef = db.collection("account").document(email).collection("realTimeData").document()
        realTimeDataRef.set(realTimeData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}