package com.kisayo.sspurt.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.kisayo.sspurt.data.ExerciseRecord
import kotlinx.coroutines.launch

class RecordViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // Firestore 오프라인 캐시 설정 활성화
    init {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // 오프라인 시 로컬 데이터 사용 설정
            .build()
        db.firestoreSettings = settings
    }

    // 라이브 데이터로 경로 리스트 관리
    private val _routePoints = MutableLiveData<MutableList<LatLng>>()
    val routePoints: LiveData<MutableList<LatLng>> get() = _routePoints

    // 녹화 상태를 관리하는 변수
    private val _isRecording = MutableLiveData<Boolean>(false)
    val isRecording: LiveData<Boolean> get() = _isRecording

    private val _isPaused = MutableLiveData<Boolean>().apply { value = false }
    val isPaused: LiveData<Boolean> = _isPaused


    // Firestore에서 사용자 경로 데이터 가져오기
    fun fetchRoute(email: String, context: Context) {
        viewModelScope.launch {
            if (isNetworkAvailable(context)) {
                // 네트워크가 가능할 때 Firestore에서 데이터 가져오기
                db.collection("account")
                    .document(email)
                    .collection("exerciseData")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val document = documents.first()
                            val exerciseRecord = document.toObject(ExerciseRecord::class.java)

                            // `routes` 데이터를 `LatLng` 리스트로 변환
                            val routeLatLngs = exerciseRecord.routes.map { latLngWrapper ->
                                LatLng(
                                    latLngWrapper.latitude,
                                    latLngWrapper.longitude
                                )
                            }

                            // `routePoints` 업데이트
                            _routePoints.postValue(routeLatLngs.toMutableList())
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("RecordViewModel", "Error fetching route: ${exception.message}")
                    }
            } else {
                // 네트워크가 불안정할 때 로컬 캐시에서 데이터 가져오기
                db.collection("account")
                    .document(email)
                    .collection("exerciseData")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get(Source.CACHE) // 로컬 캐시 사용
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val document = documents.first()
                            val exerciseRecord = document.toObject(ExerciseRecord::class.java)

                            // `routes` 데이터를 `LatLng` 리스트로 변환
                            val routeLatLngs = exerciseRecord.routes.map { latLngWrapper ->
                                LatLng(latLngWrapper.latitude, latLngWrapper.longitude)
                            }

                            // `routePoints` 업데이트
                            _routePoints.postValue(routeLatLngs.toMutableList())
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("RecordViewModel", "Error fetching route from cache: ${exception.message}")
                    }
            }
        }
    }
    // 경로 추가 메서드
    fun addRoutePoint(latLng: LatLng) {
        val updatedPoints = _routePoints.value ?: mutableListOf()
        updatedPoints.add(latLng)
        _routePoints.postValue(updatedPoints)
    }

    // 녹화 시작
    fun startRecording() {
        if (_isRecording.value == false) {
            _isRecording.value = true
            Log.d("RecordViewModel", "Recording started")
        }
    }

    // 녹화 일시 중지
    fun pauseRecording() {
        if (_isRecording.value == true) {
            _isPaused.value = true
            Log.d("RecordViewModel", "Recording paused")
        }
    }



    // 녹화 중지
    fun stopRecording() {
        if (_isRecording.value == true) {
            _isRecording.value = false
            Log.d("RecordViewModel", "Recording stopped")
        }
    }

    // 네트워크 연결 상태 확인 함수
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}