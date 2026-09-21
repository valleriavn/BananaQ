package model

data class DiseaseInfo(
    val diseaseName: String,
    val scientificName: String,
    val symptoms: List<DiseaseTip>,
    val treatment: List<DiseaseTip>,
    val prevention: List<DiseaseTip>
)

data class DiseaseTip(
    val title: String,
    val description: String
)