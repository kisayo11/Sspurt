package com.kisayo.sspurt.fragments


import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.FirebaseStorage
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.data.LatLngWrapper
import com.kisayo.sspurt.databinding.FragmentPostDialogBinding
import com.kisayo.sspurt.Helpers.FirestoreHelper
import com.kisayo.sspurt.Helpers.PlacesHelper
import com.kisayo.sspurt.utils.UserRepository
import java.io.File
import java.io.FileOutputStream
import java.util.UUID


class PostDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentPostDialogBinding
    private lateinit var placesHelper: PlacesHelper
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var userRepository: UserRepository
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        userRepository = UserRepository(requireContext())

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(onCreateView(LayoutInflater.from(context), null, savedInstanceState))
            .create()

        dialog.setOnShowListener {
            // 다이얼로그의 confirm_button을 찾기
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) // AlertDialog.BUTTON_POSITIVE 사용
            confirmButton?.setOnClickListener {
                // 다이얼로그 종료
                dialog.dismiss()

                // 현재 액티비티 종료
                // requireActivity().finish()

                // 모든 액티비티 종료 후 메인 액티비티로 이동
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        // 사진 등록 버튼 클릭 리스너
        binding.upBtn.setOnClickListener {
            Log.d("PostDialogFragment", "Upload button clicked")
            showImageSelectionDialog()
        }

        // 스피너 아이템 선택 리스너 추가
        binding.locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // 스피너에서 선택된 아이템 가져오기
                val selectedPlace = parent.getItemAtPosition(position).toString()
                // 선택된 장소에 대한 동작 추가
                Toast.makeText(requireContext(), "Selected Place: $selectedPlace", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않은 경우 처리
            }
        }

        // 저장(확인) 버튼 클릭 리스너
        binding.confirmButton.setOnClickListener {
            binding.locationSpinner.performClick()

//            handleConfirm()  // 데이터 저장 처리
        }


        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostDialogBinding.inflate(inflater, container, false)
        return binding.root

        // SharedPreferences 초기화
        sharedPreferences = requireContext().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE)

        // UserRepository 초기화
        userRepository = UserRepository(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)





        // FirestoreHelper와 PlacesHelper 초기화
        firestoreHelper = FirestoreHelper()
        val apiKey = "AIzaSyB4bm_PKHQsTeC7iBPbuJdcRat5YpDYCUs" // 실제 API 키로 대체
        placesHelper = PlacesHelper(requireContext(), apiKey)

        // 스피너 설정 및 데이터 로드
        fetchLocationDataAndSetupSpinner()

    }

    private fun fetchLocationDataAndSetupSpinner() {
        val email = userRepository.getCurrentUserEmail()

        if (email != null) {
            firestoreHelper.getUserLocationData(email,
                onSuccess = { locationWrapper ->
                    if (locationWrapper != null) {
                        // LatLngWrapper 객체로 주변 장소 가져와 스피너 설정
                        fetchNearbyPlacesAndSetupSpinner(locationWrapper)
                    } else {
                        Log.e("PostDialogFragment", "Location data not found for user")
                        Toast.makeText(requireContext(), "위치 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { exception ->
                    Log.e("PostDialogFragment", "Failed to fetch location data: ${exception.message}")
                    Toast.makeText(requireContext(), "위치 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Log.e("PostDialogFragment", "User email not found")
            Toast.makeText(requireContext(), "사용자 이메일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchNearbyPlacesAndSetupSpinner(locationWrapper: LatLngWrapper) {
        placesHelper.getNearbyPlaces(locationWrapper) { placeList ->
            setupLocationSpinner(placeList)
        }
    }
    // 스피너 설정
    private fun setupLocationSpinner(placeList: List<String>) {
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, placeList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.locationSpinner.adapter = spinnerAdapter
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
    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }
    private fun captureImage(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val imageUri = data?.data
                    if (imageUri != null) {
                        // 이미지 URI를 비트맵으로 변환하고 압축하여 업로드
                        val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                        val getImageUri = getImageUri(bitmap) // 압축된 이미지 URI
                        uploadPhoto(getImageUri) // Firebase에 업로드

                        // 이미지 뷰 업데이트 생략
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val imageUri = getImageUri(imageBitmap)
                    uploadPhoto(imageUri) // URI를 업로드 함수로 넘김

                }
            }
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "temp_photo.jpg")
        FileOutputStream(file).use{out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return Uri.fromFile(file)
    }

    private fun uploadPhoto(imageUri: Uri) {
        val email = userRepository.getCurrentUserEmail() // 현재 사용자 이메일 가져오기

        if (email != null) {
            uploadImageToFirebaseStorage(imageUri, email) // Firebase Storage에 업로드 호출
        } else {
            Toast.makeText(requireContext(), "사용자 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri, email: String) {
        val storageReference = FirebaseStorage.getInstance().getReference("images/${UUID.randomUUID()}")
        storageReference.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    // Firestore에 URL 저장
                    FirestoreHelper().saveImageUrl(email, uri.toString(),
                        onSuccess = {
                            // UI 스레드에서 토스트 표시
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "이미지가 Firestore에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFailure = { exception ->
                            Log.e("Firestore", "Failed to save image URL: ${exception.message}")
                            // UI 스레드에서 토스트 표시
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "이미지 URL 저장 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UploadImage", "Failed to upload image: ${exception.message}")
                // UI 스레드에서 토스트 표시
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 저장 버튼 눌렀을 때의 처리
//    private fun handleConfirm() {
//        val builder = AlertDialog.Builder(requireContext())
//        builder.setMessage("사진을 저장하시겠습니까?")
//        builder.setPositiveButton("예") { dialog, _ ->
//            // 저장 로직 수행
//            dialog.dismiss()
//        }
//        builder.setNegativeButton("아니요") { dialog, _ ->
//            dialog.dismiss()
//        }
//        builder.show() // 두 번째 다이얼로그 표시
//
//
//        val isShared = binding.shareSwitch.isChecked  // 공유 여부
//        val locationTag = binding.locationSpinner.selectedItem.toString()  // 선택한 위치 태그
//        val photoUrl = selectedImageUri.toString()  // 선택한 이미지의 URI
//
//        Log.d("PostDialogFragment", "Shared: $isShared, Location: $locationTag, Photo URL: $photoUrl")
//
//        // 데이터를 서버(MySQL 등)에 저장하거나 처리하는 로직 추가
//        // 여기서 MySQL에 저장하는 로직을 추가해야 함 (Retrofit, HttpURLConnection 등 사용 가능)
//    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == IMAGE_PICK_CODE || requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 선택한 작업 수행
                if (requestCode == IMAGE_PICK_CODE) {
                    pickImageFromGallery()
                } else {
                    captureImage()
                }
            } else {
                Toast.makeText(requireContext(), "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val IMAGE_PICK_CODE = 1000 // 이미지 선택 코드
        const val CAMERA_REQUEST_CODE = 1001 // 카메라 촬영 코드
    }
}