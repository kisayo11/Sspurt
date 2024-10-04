package com.kisayo.sspurt.fragments


import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.GpsConfirmActivity
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.databinding.FragmentPostDialogBinding


class postDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentPostDialogBinding
    private var selectedImageUri: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(
                onCreateView(
                    LayoutInflater.from(context), null, savedInstanceState
                )
            ).create()

        dialog.setOnShowListener {
            // 다이얼로그의 confirm_button을 찾기
            val confirmButton = dialog.findViewById<Button>(R.id.confirm_button)
            if (confirmButton != null) {
                confirmButton.setOnClickListener {
                    // 다이얼로그 종료
                    dialog.dismiss()

                    // 현재 액티비티 종료
                    requireActivity().finish()

                    // 모든 액티비티 종료 후 메인 액티비티로 이동
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 사진 등록 버튼 클릭 리스너
        binding.upBtn.setOnClickListener {
            Log.d("PostDialogFragment", "Upload button clicked")
            showImageSelectionDialog()
        }

        // 저장(확인) 버튼 클릭 리스너
        binding.confirmButton.setOnClickListener {
//            handleConfirm()  // 데이터 저장 처리
        }
    }

    // 이미지 선택 창 열기
    private fun showImageSelectionDialog() {
        val options = arrayOf("사진 선택", "사진 촬영")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("이미지 선택")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> pickImageFromGallery() // 사진 선택
                1 -> captureImage() // 사진 촬영
            }
        }
        builder.show()
    }

    // 이미지 선택 후 결과 처리
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val imageUri = data?.data
                    // URI를 사용하여 이미지 처리
                    if (imageUri != null) {
                        // 이미지를 설정하거나 업로드하는 로직 추가
                    }
                }

                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    // 비트맵을 사용하여 이미지 처리
                }
            }
        }
    }

//    // 저장 버튼 눌렀을 때의 처리
//    private fun handleConfirm() {
//        val isShared = binding.shareSwitch.isChecked  // 공유 여부
//        val locationTag = binding.locationSpinner.selectedItem.toString()  // 선택한 위치 태그
//        val photoUrl = selectedImageUri.toString()  // 선택한 이미지의 URI
//
//        Log.d("PostDialogFragment", "Shared: $isShared, Location: $locationTag, Photo URL: $photoUrl")
//
//        // 데이터를 서버(MySQL 등)에 저장하거나 처리하는 로직 추가
//        // 여기서 MySQL에 저장하는 로직을 추가해야 함 (Retrofit, HttpURLConnection 등 사용 가능)
//    }

    companion object {
        const val IMAGE_PICK_CODE = 1000 // 이미지 선택 코드
        const val CAMERA_REQUEST_CODE = 1001 // 카메라 촬영 코드
    }
}