package com.kisayo.sspurt.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
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
        binding.profileCv.setOnClickListener {
            showImageSelectionDialog()
        }

        //로그아웃 리스너
        binding.logOutTv.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        //회원탈퇴 리스너
        binding.deleteIdTv.setOnClickListener {
            showDeleteUserConfirmationDialog()
        }

        //비밀번호 재설정 리스너
        binding.resetPasswordTv.setOnClickListener {
            showResetPasswordDialog()
        }

        //사용자 데이터 로드
        fetchUserData()
    }

    private fun fetchUserData() {
        val email = userRepository.getCurrentUserEmail()
        email?.let {
            userRepository.fetchUserData(it) { imageUrl ->
                if (imageUrl != null) {
                    // Firebase에서 가져온 이미지 URL로 설정
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
    private  fun showDeleteUserConfirmationDialog(){
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    imageUri = data?.data!! // 선택한 이미지 URI
                    uploadProfileImage(imageUri) // 이미지 업로드
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imageUri = getImageUri(imageBitmap) // 비트맵을 URI로 변환
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
        imageUploader.uploadProfileImage(imageUri) { imageUrl ->
            val email = userRepository.getCurrentUserEmail()
            if (email != null && imageUrl != null) {
                userRepository.updateProfileImageUrl(email, imageUrl)

                // SharedPreferences에 이미지 URL 저장
                val editor = sharedPreferences.edit()
                editor.putString("profileImageUrl", imageUrl)
                editor.apply()

                Glide.with(this).load(imageUrl).into(binding.profileCv) // 이미지 뷰 업데이트
            } else {
                Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
            }
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