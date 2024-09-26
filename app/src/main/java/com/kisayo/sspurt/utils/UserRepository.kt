package com.kisayo.sspurt.utils


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(private val context : Context) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("LoginInfo", Context.MODE_PRIVATE)

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun fetchUserData(email: String, onSuccess: (String?) -> Unit) {
        db.collection("account").document(email).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageUrl = document.getString("profileImageUrl")
                    onSuccess(imageUrl)
                } else {
                    onSuccess(null)
                }
            }
    }

    fun updateProfileImageUrl(email: String, imageUrl: String) {
        db.collection("account").document(email).update("profileImageUrl", imageUrl)
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


}
