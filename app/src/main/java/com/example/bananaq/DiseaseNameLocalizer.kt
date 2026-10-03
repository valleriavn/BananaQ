package com.example.bananaq

import android.content.Context

fun localizedDiseaseName(context: Context, canonicalName: String): String {
    val resourceId = when (canonicalName) {
        "Healthy" -> R.string.disease_healthy
        "Black Sigatoka" -> R.string.disease_black_sigatoka
        "Panama Disease" -> R.string.disease_panama
        "Cordana Leaf Spot" -> R.string.disease_cordana
        else -> return canonicalName
    }
    return context.getString(resourceId)
}
