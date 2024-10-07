package com.kisayo.sspurt.Helpers

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

object PolylineHelper {

    fun getPolylineOptionsByExerciseType(exerciseType: String): PolylineOptions {
        return when (exerciseType) {
            "running" -> PolylineOptions().color(Color.BLUE).width(12f)
            "cycling" -> PolylineOptions().color(Color.GREEN).width(10f)
            "hiking" -> PolylineOptions().color(Color.rgb(139, 69, 19)).width(15f)
            "trailrunning" -> PolylineOptions().color(Color.MAGENTA).width(14f)
            else -> PolylineOptions().color(Color.GRAY).width(10f)
        }
    }

    fun simplifyCoordinates(coordinates: List<LatLng>, interval: Int = 10): List<LatLng> {
        val simplifiedCoordinates = mutableListOf<LatLng>()
        for (i in coordinates.indices step interval) {
            simplifiedCoordinates.add(coordinates[i])
        }
        return simplifiedCoordinates
    }
}