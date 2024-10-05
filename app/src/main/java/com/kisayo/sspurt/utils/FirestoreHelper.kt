package com.kisayo.sspurt.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    // 최근 운동 기록 삭제
    fun deleteRecentExerciseRecord(email: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val recentRecordRef = db.collection("account").document(email).collection("exerciseData")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)

        recentRecordRef.get()
            .addOnSuccessListener { documents ->
                Log.d("FirestoreHelper", "문서 수: ") // 문서 수 확인
                if (documents.isEmpty) {
                    Log.d("FirestoreHelper", "no record for delete")
                    onFailure(Exception("삭제할 기록이 없습니다."))
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    Log.d("FirestoreHelper", "삭제할 문서 ID: ${document.id}, timestamp: ${document.getTimestamp("timestamp")}") // 문서 정보 출력
                    db.collection("account").document(email).collection("exerciseData").document(document.id).delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreHelper", "삭제 실패: ${e.message}")
                            onFailure(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "문서 가져오기 실패: ${e.message}")
                onFailure(e)
            }
    }

    fun saveImageUrl(email: String, imageUrl: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Firestore에서 해당 사용자의 최신 운동 기록을 업데이트
        val userRecordRef = db.collection("account").document(email).collection("exerciseData").document("latestRecord") // 최신 기록에 저장 (예시)

        userRecordRef.update("photoUrl", imageUrl)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

}