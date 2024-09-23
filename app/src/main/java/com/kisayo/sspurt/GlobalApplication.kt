package com.kisayo.sspurt

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kisayo.sspurt.data.UserAccount

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //Kakao login 초기화
        KakaoSdk.init(this,"489c032488f2fc53c7acc6dd2deafabc")
    }
}

class G {
    companion object{
        var userAccount: UserAccount?=null
    }
}

class Constants{
    companion object{
        const val ACCOUNT_COLLECTION = "account"
    }
}