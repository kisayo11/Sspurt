package com.kisayo.sspurt.utils


import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("LoginInfo", Context.MODE_PRIVATE)

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun fetchUserData(email: String, onSuccess: (username: String?, imageUrl: String?) -> Unit) {
        db.collection("account").document(email).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    val imageUrl = document.getString("profileImageUrl")
                    onSuccess(username, imageUrl)
                } else {
                    onSuccess(null, null)
                }
            }
    }

    fun updateProfileImageUrl(email: String, imageUrl: String, onComplete: (Boolean) -> Unit) {
        db.collection("account").document(email).update("profileImageUrl", imageUrl)
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun logout() {
        try {
            auth.signOut() // Firebase에서 로그아웃
            // SharedPreferences 초기화
            val editor = sharedPreferences.edit()
            editor.putBoolean("AutoLoginChecked", false) // 자동 로그인 체크 해제
            editor.apply()
        } catch (e: Exception) {
            Log.e("UserRepository", "Logout error: ${e.message}")
        }
    }

    fun deleteUser(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        user?.delete()?.addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun updatePassword(email: String, newPassword: String, callback: (Boolean) -> Unit) {
        val user = auth.currentUser
        user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun updateUsername(email: String, newUsername: String, onComplete: (Boolean) -> Unit) {
        db.collection("account").document(email).update("username", newUsername)
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun uploadProfileImage(userId: String, imageUri: Uri, email: String, onComplete: (Boolean) -> Unit) {
        val filePath = "profileImages/$userId.jpg" // 같은 경로와 파일명으로 설정
        val storageReference = FirebaseStorage.getInstance().getReference(filePath)

        storageReference.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // 업로드 성공 시 URL 가져오기
                storageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Firestore에 URL 업데이트
                    updateProfileImageUrl(email, downloadUrl.toString()) { success ->
                        onComplete(success) // Firestore 업데이트 성공 여부 전달
                    }
                }
            }
            .addOnFailureListener { exception ->
                onComplete(false) // 실패 시 false 반환
            }
    }
}
