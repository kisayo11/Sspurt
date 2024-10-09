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
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.kisayo.sspurt.Helpers.FirestoreHelper
import com.kisayo.sspurt.Helpers.PlacesHelper
import com.kisayo.sspurt.R
import com.kisayo.sspurt.activities.MainActivity
import com.kisayo.sspurt.data.ExerciseRecord
import com.kisayo.sspurt.databinding.FragmentPostDialogBinding
import com.kisayo.sspurt.utils.UserRepository
import java.io.File
import java.io.FileOutputStream
import java.util.UUID


class PostDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentPostDialogBinding
    private lateinit var placesHelper: PlacesHelper
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userRepository: UserRepository
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var exerciseData: ExerciseRecord
    private lateinit var exerciseRecordId: String
    private var isShared = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        firestore = FirebaseFirestore.getInstance()
        userRepository = UserRepository(requireContext())
        exerciseRecordId = arguments?.getString("exerciseRecordId") ?: ""

        // 다이얼로그에 사용할 뷰를 인플레이트
        binding = FragmentPostDialogBinding.inflate(LayoutInflater.from(context))

        // 다이얼로그 생성
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root) // 다이얼로그에 뷰 설정
            .setPositiveButton("확인", null) // Positive 버튼 추가
            .setCancelable(false) // 다이얼로그 외부 클릭 무시
            .create()

        // Firestore 또는 다른 곳에서 데이터를 불러와서 exerciseData를 설정
        exerciseData = ExerciseRecord() // Firestore에서 불러온 데이터를 여기에 설정

        // 스위치 상태가 변경될 때 로그로 확인 (체크될 때마다 기록)
        binding.shareSwitch.setOnCheckedChangeListener { _, isChecked ->
            isShared = isChecked
            Log.d("SwitchState", "스위치 상태 변경: isShared = $isChecked")
        }

        // 다이얼로그가 표시되었을 때 호출
        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            confirmButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            confirmButton.textSize = 14f
            val typeface=ResourcesCompat.getFont(requireContext(),R.font.pureunjeonnam)
            confirmButton.typeface = typeface
            // 버튼 모서리 둥글게 설정하고 패딩 추가 (버튼처럼 보이게)
            confirmButton.setPadding(40, 20, 40, 20) // 패딩 설정
            confirmButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))

            confirmButton.setOnClickListener {

                // Firestore에 저장
                val email = userRepository.getCurrentUserEmail()
                val db = FirebaseFirestore.getInstance()

                val userRecordRef = db.collection("account")
                    .document(email!!)
                    .collection("exerciseData")
                    .document(exerciseRecordId)

                userRecordRef.update("isShared", isShared)
                    .addOnSuccessListener {
                        Log.d("Firestore", "isShared 필드가 Firestore에 성공적으로 저장되었습니다: $isShared")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "isShared 필드 저장 실패: ${e.message}")
                    }

                // 다이얼로그 닫기
                dialog.dismiss()

                // 현재 액티비티 종료
                requireActivity().finish()

                // 메인 액티비티로 이동
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        // 다이얼로그의 뷰에서 버튼 클릭 리스너 설정
        binding.upBtn.setOnClickListener {
            Log.d("PostDialogFragment", "Upload button clicked")
            showImageSelectionDialog() // 이미지 선택 다이얼로그 호출
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

    }

    // Firestore에 isShared 값을 저장하는 함수
    private fun saveIsSharedToFirestore(isShared: Boolean) {
        val email = userRepository.getCurrentUserEmail()
        if (email != null) {
            val db = FirebaseFirestore.getInstance()
            // Firestore 컬렉션 경로
            val userRecordRef = db.collection("account")
                .document(email)
                .collection("exerciseData")
                .document(exerciseRecordId)


            Log.d("FirestoreSave", "Firestore 문서 경로: account/$email/exerciseData/$exerciseRecordId")
            Log.d("FirestoreSave", "저장하기 직전 isShared 값: $isShared")


            Log.d("FirestoreSave", "Save before isShared 값: $isShared")

            userRecordRef.update("isShared", isShared)
                .addOnSuccessListener {
                    Log.d("Firestore", "isShared Field saved success.")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "isShared 필드 저장 실패: ${e.message}")
                }
        } else {
            Toast.makeText(requireContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
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
                    if (imageUri != null) {
                        // 이미지 URI를 비트맵으로 변환하고 압축하여 업로드
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            requireContext().contentResolver,
                            imageUri
                        )
                        val getImageUri = getImageUri(bitmap) // 압축된 이미지 URI
                        uploadPhoto(
                            getImageUri,
                            exerciseData.exerciseRecordId
                        ) // exerciseRecordId 전달

                        // 이미지 뷰 업데이트 생략
                    }
                }

                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val imageUri = getImageUri(imageBitmap)
                    uploadPhoto(imageUri, exerciseData.exerciseRecordId) // URI를 업로드 함수로 넘김
                }
            }
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "temp_photo.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return Uri.fromFile(file)
    }

    private fun uploadPhoto(imageUri: Uri, exerciseRecordId: String) {
        val email = userRepository.getCurrentUserEmail() // 현재 사용자 이메일 가져오기

        if (email != null) {
            // Firebase Storage에 업로드 호출, exerciseRecordId 전달
            uploadImageToFirebaseStorage(imageUri, email, exerciseRecordId)
        } else {
            Toast.makeText(requireContext(), "사용자 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // Firebase Storage에 이미지 업로드 및 Firestore에 URL 저장
    private fun uploadImageToFirebaseStorage(
        imageUri: Uri,
        email: String,
        exerciseRecordId: String
    ) {
        val storageReference =
            FirebaseStorage.getInstance().getReference("images/${UUID.randomUUID()}")

        // Firebase Storage에 이미지 파일 업로드
        storageReference.putFile(imageUri).addOnSuccessListener {
                // 업로드 성공 로그
                Log.d("UploadImage", "Image successfully uploaded to Firebase Storage.")

                // 업로드 성공 후, 다운로드 URL 획득
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString() // 이미지 다운로드 URL
                    Log.d("DownloadURL", "Download URL successfully obtained: $downloadUrl")

                    // Firestore에 URL 저장
                    saveImageUrlToFirestore(downloadUrl)
                }.addOnFailureListener { exception ->
                    // 다운로드 URL을 가져오는 데 실패한 경우
                    Log.e("DownloadURL", "Failed to get download URL: ${exception.message}")
                    Toast.makeText(requireContext(), "이미지 URL을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }.addOnFailureListener { exception ->
                // 이미지 업로드 실패 로그
                Log.e("UploadImage", "Failed to upload image: ${exception.message}")
                Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestore에 다운로드 URL 저장
    private fun saveImageUrlToFirestore(downloadUrl: String) {
        val email = userRepository.getCurrentUserEmail() // 현재 로그인된 사용자의 이메일 가져오기

        if (email != null) {
            val db = FirebaseFirestore.getInstance()

            // Firestore 문서 경로 설정
            val userRecordRef = db.collection("account")
                .document(email)
                .collection("exerciseData")
                .document(exerciseRecordId) // exerciseRecordId를 문서 ID로 사용

            // 다운로드 URL을 Firestore 문서에 저장
            userRecordRef.update("photoUrl", downloadUrl)
                .addOnSuccessListener {
                    Log.d("Firestore", "Image URL saved successfully.")
                    Toast.makeText(requireContext(), "이미지가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Failed to save image URL: ${exception.message}")
                    Toast.makeText(requireContext(), "이미지 URL 저장 실패", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("Firestore", "User email is null. Cannot proceed with Firestore operations.")
            Toast.makeText(requireContext(), "사용자 이메일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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