package com.kisayo.sspurt.networks

import com.kisayo.sspurt.data.NidUserInfoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface RetrofitService {
    //naver login  api
    //json을 NidUserInfoResponce 라는 데이터클래스 객체로 파싱하여 받아오시오
    @GET("/v1/nid/me")
    fun getNidUserInfo(@Header("Authorization") authorization:String):Call<NidUserInfoResponse>

    //카카오 키워드 장소 검색 api를 get 방식으로 요청하는 작업명세
//    @Headers("Authorization: KakaoAK b448fc6607291705d8f2d2501f555590")
//    @GET("/v2/local/search/keyword.json?sort=distance")
//    fun searchPlacesFromServer(@Query("query") query: String, @Query("x") longitude:String, @Query("y") latitude:String) : Call<String>
//
//    @Headers("Authorization: KakaoAK b448fc6607291705d8f2d2501f555590")
//    @GET("/v2/local/search/keyword.json?sort=distance")
//    fun searchPlacesFromServer2(@Query("query") query: String, @Query("x") longitude:String, @Query("y") latitude:String) : Call<KakaoSearchPlaceResponce>
}