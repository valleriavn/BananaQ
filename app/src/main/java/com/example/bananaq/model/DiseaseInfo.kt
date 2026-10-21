package com.example.bananaq.model

import com.google.gson.annotations.SerializedName

data class DiseaseInfo(
    @SerializedName("diseaseName")
    val diseaseName: String,
    @SerializedName("scientificName")
    val scientificName: String,
    @SerializedName("symptoms")
    val symptoms: List<DiseaseTip>,
    @SerializedName("treatment")
    val treatment: List<DiseaseTip>,
    @SerializedName("prevention")
    val prevention: List<DiseaseTip>
)

data class DiseaseTip(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("id")
    val id: String = "",
    @SerializedName("essential")
    val essential: Boolean = false
)
