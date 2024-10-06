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
import com.kisayo.sspurt.data.LatLngWrapper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class PlacesHelper(private val context: Context, apiKey: String) {

    init {
        // Places API 초기화
        Places.initialize(context.applicationContext, apiKey)
    }

    /**
     * 위치에 기반한 주변 장소 정보를 가져오는 메서드
     */
    fun getNearbyPlaces(locationWrapper: LatLngWrapper, callback: (List<String>) -> Unit) {
        val location = LatLng(locationWrapper.latitude, locationWrapper.longitude)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 필터링할 장소 유형과 반경 설정
            val types = "park|mountain|subway_station|gym" // 필터링할 장소 유형
            val radius = 5000 // 5km
            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${location.latitude},${location.longitude}&radius=$radius&type=$types&key=AIzaSyB4bm_PKHQsTeC7iBPbuJdcRat5YpDYCUs"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")

                        val jsonResponse = response.body?.string()
                        Log.d("Places API Response", jsonResponse ?: "No response")

                        // API 응답을 파싱하여 장소 목록을 가져옴
                        val placeList = parsePlaces(jsonResponse)
                        callback(placeList)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Places API", "Place not found: ${e.message}")
                    callback(emptyList())
                }
            })
        } else {
            Log.e("PlacesHelper", "Location permission not granted")
            callback(emptyList())
        }
    }

    // 장소 정보를 파싱하는 메서드 (JSON 파싱)
    private fun parsePlaces(jsonResponse: String?): List<String> {
        val placeList = mutableListOf<String>()

        jsonResponse?.let {
            try {
                val jsonObject = JSONObject(it)
                val results = jsonObject.getJSONArray("results")

                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val name = place.getString("name") // 장소 이름
                    placeList.add(name) // 장소 이름을 리스트에 추가
                }
            } catch (e: JSONException) {
                Log.e("Places API", "JSON parsing error: ${e.message}")
            }
        }

        return placeList // 장소 이름 리스트 반환
    }
}