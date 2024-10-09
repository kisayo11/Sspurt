package com.kisayo.sspurt.activities

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

import com.kisayo.sspurt.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val ACTION_START = "ACTION_START"
    private val ACTION_STOP = "ACTION_STOP"
    private val TAG = "TrackingService"
    private val NOTIFICATION_ID = 121
    private var notificationManager: NotificationManagerCompat? = null


    // 코루틴 범위 지정
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    // 서비스가 생성될 때 호출되는 메서드
    override fun onCreate() {
        super.onCreate()
        // Google FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // NotificationManagerCompat 초기화
        notificationManager = NotificationManagerCompat.from(this)
        // NotificationChannel 생성
        createNotificationChannel()
    }

    // 서비스가 시작될 때 호출되는 메서드
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 알림 생성 및 포그라운드 시작
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        when (intent?.action) {
            ACTION_START -> {
                startLocationTracking()
            }
            ACTION_STOP -> {
                stopLocationTracking()
                stopSelf()
            }
        }
        return START_STICKY
    }


    // 위치 추적 시작 (코루틴을 사용해 비동기 작업 처리)
    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        // 코루틴 내에서 위치 추적 처리
        serviceScope.launch {
            val locationRequest = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            Log.d(TAG, "Location tracking started (Coroutine)")
        }
    }

    // 위치 추적을 중지하는 메서드
    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        Log.d(TAG, "Location tracking stopped")
    }

    // NotificationChannelCompat을 사용하여 채널 생성 (Android 8.0 이상)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_channel",
                "Tracking Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("Sspurt")
            .setContentText("운동 추적중입니다.")
            .setSmallIcon(R.drawable.logo_sspurt) // 아이콘 설정
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // 위치 업데이트를 처리하는 콜백
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            if (locationResult.locations.isNotEmpty()) {
                val currentLocation = locationResult.locations.last()
                Log.d(TAG, "Location updated: ${currentLocation.latitude}, ${currentLocation.longitude}")
            }
        }
    }

    // 서비스가 중지될 때 호출되는 메서드
    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        Log.d(TAG, "Service destroyed")
    }

    // 서비스 바인딩 (사용하지 않음)
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}