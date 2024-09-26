package com.kisayo.sspurt.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kisayo.sspurt.activities.login.LoginActivity
import com.kisayo.sspurt.databinding.FragmentMyAccountBinding
import com.kisayo.sspurt.utils.ImageUploader
import com.kisayo.sspurt.utils.UserRepository
import java.io.File
import java.io.FileOutputStream


class MyAccountFragment : Fragment() {

    private var _binding: FragmentMyAccountBinding? = null
    private val binding get() = _binding!!

    private lateinit var imageUri: Uri
    private lateinit var userRepository: UserRepository
    private val imageUploader = ImageUploader()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAccountBinding.inflate(inflater,container,false)
        return binding.root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SharedPreferences 초기화
        sharedPreferences = requireContext().getSharedPreferences("LoginInfo", Context.MODE_PRIVATE)

        // UserRepository 초기화
        userRepository = UserRepository(requireContext())

        //프로필사진변경
        binding.profileCv.setOnClickListener {showImageSelectionDialog()}
        //로그아웃 리스너
        binding.logOutTv.setOnClickListener {showLogoutConfirmationDialog()}
        //회원탈퇴 리스너
        binding.deleteIdTv.setOnClickListener {showDeleteUserConfirmationDialog()}
        //비밀번호 재설정 리스너
        binding.resetPasswordTv.setOnClickListener {showResetPasswordDialog()}
        //유저네임 변경 리스너
        binding.changeUsername.setOnClickListener { changeUsernameDialog() }

        //사용자 데이터 로드
        fetchUserData()

        //이메일 불러오기 sharedpreference
        val savedEmail = sharedPreferences.getString("savedEmail",null)
        if(savedEmail != null){
            binding.tvUserEmail.text = savedEmail
            loadUsernameFromFirestore(savedEmail)
        }

       }

    private fun fetchUserData() {
        val email = userRepository.getCurrentUserEmail()
        email?.let {
            userRepository.fetchUserData(it) { username, imageUrl -> // 사용자 이름과 이미지 URL을 받아옵니다
                // 사용자 이름 설정
                if (username != null) {
                    binding.tvUserName.text = username
                }

                // 프로필 이미지 설정
                if (imageUrl != null) {
                    Glide.with(this).load(imageUrl).into(binding.profileCv)
                } else {
                    // SharedPreferences에서 가져온 이미지 URL로 설정
                    val savedImageUrl = sharedPreferences.getString("profileImageUrl", null)
                    if (savedImageUrl != null) {
                        Glide.with(this).load(savedImageUrl).into(binding.profileCv)
                    }
                }
            }
        }
    }

    private fun showImageSelectionDialog(){
        val options = arrayOf("사진 선택","사진 촬영")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("프로필 이미지 선택")
        builder.setItems(options) { _, which ->
            when(which){
                0 -> pickImageFromGallery() // 사진 선택
                1 -> captureImage() // 사진 촬영
            }
        }
        builder.show()
    }
    private fun pickImageFromGallery(){
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }
    private fun captureImage(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }
    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(requireActivity()) // requireContext() 대신 requireActivity() 사용
        builder.setTitle("로그아웃")
        builder.setMessage("정말로 로그아웃 하시겠습니까?")
        builder.setPositiveButton("예") { _, _ ->
            userRepository.logout() // UserRepository의 logout 호출
            Toast.makeText(requireActivity(), "로그아웃 성공", Toast.LENGTH_SHORT).show() // requireActivity() 사용
            val intent = Intent(requireActivity(), LoginActivity::class.java) // requireActivity() 사용
            startActivity(intent)
            requireActivity().finish() // 현재 액티비티 종료
        }
        builder.setNegativeButton("아니오", null)
        builder.show()
    }
    private fun showDeleteUserConfirmationDialog(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("회원탈퇴")
        builder.setMessage("정말로 회원탈퇴를 하시겠습니까?")
        builder.setPositiveButton("예") { _, _->
            userRepository.deleteUser { isSuccess ->
                if(isSuccess){
                }
            }
        }
        builder.setNegativeButton("아니오",null)
        builder.show()
    }
    private fun showResetPasswordDialog() {
        // 비밀번호 입력을 위한 EditText 생성
        val editText = EditText(requireContext())
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("비밀번호 재설정")
        builder.setMessage("새로운 비밀번호를 입력하세요.")
        builder.setView(editText) // EditText를 다이얼로그에 추가
        builder.setPositiveButton("확인") { _, _ ->
            val newPassword = editText.text.toString()
            val email = userRepository.getCurrentUserEmail()
            if (email != null) {
                userRepository.updatePassword(email, newPassword) { isSuccess ->
                    if (isSuccess) {
                        // 비밀번호 변경 성공 메시지
                    }
                }
            }
        }
        builder.setNegativeButton("취소", null)
        builder.show()
    }
    private fun loadUsernameFromFirestore(email : String){
        val database = FirebaseFirestore.getInstance()
        database.collection("account").document(email)
            .get()
            .addOnSuccessListener { document ->
                if(document != null){
                    val username = document.getString("username")
                    binding.tvUserName.text = username
                }
            }
            .addOnFailureListener { e->
                Toast.makeText(requireContext(), "user name error", Toast.LENGTH_SHORT).show()
            }
    }
    private fun changeUsernameDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT // 텍스트 입력
            maxLines = 1 // 최대 줄 수를 1로 설정
        }
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("닉네임 재설정")
        builder.setMessage("새로운 닉네임을 설정하세요.")
        builder.setView(editText)

        builder.setPositiveButton("변경") { dialog, _ ->
            val newUsername = editText.text.toString()
            val email = userRepository.getCurrentUserEmail()

            if (email != null && newUsername.isNotBlank()) {
                // 사용자 이름 업데이트
                userRepository.updateUsername(email, newUsername) { success ->
                    if (success) {
                        binding.tvUserName.text = newUsername // UI 업데이트
                        Toast.makeText(requireContext(), "닉네임이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "닉네임 변경 오류", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "유효한 닉네임을 입력하세요.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val imageUri = data?.data // 선택한 이미지 URI
                    if (imageUri != null) {
                        uploadProfileImage(imageUri)
                    } // 이미지 업로드
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val imageUri = getImageUri(imageBitmap) // 비트맵을 URI로 변환
                    uploadProfileImage(imageUri) // 이미지 업로드
                }
            }
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "temp_image.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) // 비트맵을 JPEG 형식으로 저장
        }
        return Uri.fromFile(file) // 파일의 URI 반환
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val email = userRepository.getCurrentUserEmail() // 현재 사용자 이메일 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid // 사용자 ID 가져오기

        if (email != null && userId != null) {
            userRepository.uploadProfileImage(userId, imageUri, email) { success ->
                if (success) {
                    // 이미지 뷰 업데이트
                    Glide.with(this).load(imageUri).into(binding.profileCv)
                    Toast.makeText(requireContext(), "프로필 사진이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "사용자 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
    companion object {
        const val IMAGE_PICK_CODE = 1000 // 이미지 선택 코드
        const val CAMERA_REQUEST_CODE = 1001 // 카메라 촬영 코드
    }
}