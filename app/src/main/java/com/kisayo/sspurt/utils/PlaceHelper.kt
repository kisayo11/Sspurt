package com.kisayo.sspurt.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlacesHelper(private val context: Context, apiKey: String) {

    init {
        // Places API 초기화
        Places.initialize(context.applicationContext, apiKey)
    }

    private val placesClient: PlacesClient = Places.createClient(context)

    /**
     * 위치에 기반한 주변 장소 정보를 가져오는 메서드
     */
    fun getNearbyPlaces(location: LatLng, callback: (List<String>) -> Unit) {
        // 위치 권한 확인
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 권한이 허용된 경우 장소 검색 실행
            val placeFields = listOf(Place.Field.NAME, Place.Field.ADDRESS)
            val request = FindCurrentPlaceRequest.newInstance(placeFields)

            placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
                val placeList = response.placeLikelihoods.map { it.place.name ?: "Unknown Place" }
                callback(placeList)
            }.addOnFailureListener { exception ->
                Log.e("Places API", "Place not found: ${exception.message}")
                callback(emptyList())
            }
        } else {
            // 권한이 없는 경우 처리
            Log.e("PlacesHelper", "Location permission not granted")
            callback(emptyList())
        }
    }
}