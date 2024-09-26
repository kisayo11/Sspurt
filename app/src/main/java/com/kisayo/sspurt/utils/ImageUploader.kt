package com.kisayo.sspurt.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

class ImageUploader {

    fun uploadProfileImage(imageUri: Uri, onSuccess: (String) -> Unit) {
        val storageReference = FirebaseStorage.getInstance().reference
            .child("profile_images/${System.currentTimeMillis()}.jpg")

        storageReference.putFile(imageUri).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }
    }
}
