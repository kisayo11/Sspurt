package com.kisayo.sspurt.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.data.ExerciseRecord

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    fun saveExerciseRecord(email: String, record: ExerciseRecord, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRecordRef = db.collection("accounts").document(email).collection("exerciseData").document()
        userRecordRef.set(record)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}