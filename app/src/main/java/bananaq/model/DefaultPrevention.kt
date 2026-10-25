package bananaq.model

import com.google.gson.annotations.SerializedName

/** Always displayed after the scan's saved disease-specific prevention, never randomized. */
data class DefaultPrevention(
    @SerializedName("practices") val practices: List<DiseaseTip>,
    @SerializedName("healthyPractice") val healthyPractice: DiseaseTip,
    @SerializedName("source") val source: String,
    @SerializedName("sourceUrl") val sourceUrl: String,
    @SerializedName("requestedSourceUrl") val requestedSourceUrl: String,
    @SerializedName("verificationNote") val verificationNote: String,
    @SerializedName("office") val office: AgriculturalOffice,
    @SerializedName("contactSourceUrl") val contactSourceUrl: String
) {
    val contact: String
        get() = "${office.phone} local ${office.extension}\n${office.email}\n${office.address}\n${office.hours}"
    // These legacy general items are covered by the fixed practices. Saved text stays intact.
    fun fixedPracticesFor(info: DiseaseInfo): List<DiseaseTip> =
        if (info.diseaseName == "Healthy") practices + healthyPractice else practices

    fun diseaseSpecificPrevention(info: DiseaseInfo): List<DiseaseTip> = info.prevention.filterNot {
        it.id in coveredPreventionIds || fixedPracticesFor(info).any { practice -> practice.id == it.id }
    }

    fun prevention(info: DiseaseInfo): List<DiseaseTip> = diseaseSpecificPrevention(info) + fixedPracticesFor(info)

    fun diseaseSpecificTreatment(info: DiseaseInfo): List<DiseaseTip> = info.treatment.filterNot {
        it.id in preventiveTreatmentIds
    }

    private companion object {
        val coveredPreventionIds = setOf("panama.prevention.v2.1", "panama.prevention.v2.4", "panama.prevention.v2.8")
        val preventiveTreatmentIds = setOf("panama.treatment.v2.4", "panama.treatment.v2.5")
    }
}

/** Structured details keep the display number, extension and dialable number distinct. */
data class AgriculturalOffice(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("extension") val extension: String,
    @SerializedName("dialNumber") val dialNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("address") val address: String,
    @SerializedName("hours") val hours: String
)
