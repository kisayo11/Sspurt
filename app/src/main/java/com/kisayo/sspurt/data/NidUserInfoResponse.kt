package com.kisayo.sspurt.data

data class NidUserInfoResponse(val resultcode:String, val message:String, val response:UserInfo)
data class UserInfo(val id:String, val email:String)
