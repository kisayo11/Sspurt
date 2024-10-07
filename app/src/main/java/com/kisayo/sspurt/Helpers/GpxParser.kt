package com.kisayo.sspurt.Helpers

import android.location.Location
import android.util.Log
import android.util.Xml
import com.google.android.gms.maps.model.LatLng
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class GpxParser {

    fun parse(inputStream: InputStream): Pair<List<LatLng>, Pair<Int, Double>> {
        val coordinates = mutableListOf<LatLng>()
        var highestElevation = 0
        var totalDistance = 0.0

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var previousPoint: LatLng? = null
        var currentElevation = 0.0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("trkpt", ignoreCase = true)) {
                        val lat = parser.getAttributeValue(null, "lat").toDouble()
                        val lon = parser.getAttributeValue(null, "lon").toDouble()
                        coordinates.add(LatLng(lat, lon))

                        // 거리 계산
                        if (previousPoint != null) {
                            val distance = calculateDistance(previousPoint, LatLng(lat, lon))
                            totalDistance += distance
                        }

                        previousPoint = LatLng(lat, lon)
                    }

                    // `ele` 태그를 안전하게 파싱
                    if (tagName.equals("ele", ignoreCase = true)) {
                        try {
                            currentElevation = parser.nextText().toDouble()
                            if (currentElevation > highestElevation) {
                                highestElevation = currentElevation.toInt()
                            }
                        } catch (e: Exception) {
                            Log.e("GpxParser", "Error parsing elevation: ${e.message}")
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        // 총 거리를 km로 변환
        totalDistance /= 1000.0 // 미터를 km로 변환
        return Pair(coordinates, Pair(highestElevation, totalDistance)) // 좌표 및 최대 고도, 총 거리 반환
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0].toDouble() // 거리 반환 (미터 단위)
    }
}