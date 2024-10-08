package com.kisayo.sspurt.Helpers

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.data.LatLngWrapper
import com.kisayo.sspurt.data.RealTimeData

class FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()

    // 운동 기록 저장
    fun saveExerciseRecord(
        email: String,
        record: ExerciseRecord,
        onSuccess: (String) -> Unit, // onSuccess에서 생성된 ID를 전달
        onFailure: (Exception) -> Unit
    ) {
        val userRecordRef = db.collection("account").document(email).collection("exerciseData").document()
        record.exerciseRecordId = userRecordRef.id // Firestore에서 생성된 문서 ID를 record에 설정
        userRecordRef.set(record)
            .addOnSuccessListener {
                onSuccess(userRecordRef.id) // onSuccess로 생성된 ID 반환
            }
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

    fun getUserLocationData(email: String, onSuccess: (LatLngWrapper?) -> Unit, onFailure: (Exception) -> Unit) {
        val userDocRef = db.collection("account").document(email)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    // LatLngWrapper로 변환
                    onSuccess(LatLngWrapper(latitude, longitude))
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreHelper", "Error fetching location: ${exception.message}")
                onFailure(exception)
            }
    }



}