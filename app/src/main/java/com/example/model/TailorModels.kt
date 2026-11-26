package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class MeasurementUnit(val code: String) {
    INCHES("in"),
    CENTIMETERS("cm");

    companion object {
        fun fromCode(code: String): MeasurementUnit {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INCHES
        }
    }
}

enum class LanguageMode(val code: String) {
    BOTH("BOTH"),
    EN("EN"),
    UR("UR");

    companion object {
        fun fromCode(code: String): LanguageMode {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: BOTH
        }
    }
}

enum class CustomerSortOption {
    RECENT,
    NAME_ASC,
    CUSTOMER_CODE
}

enum class ShalwarStyleOption(val en: String, val ur: String) {
    SIMPLE("Simple Shalwar", "سادہ شلوار"),
    PATIALA("Patiala Shalwar", "پٹیالہ شلوار"),
    TROUSER("Trouser / Straight", "ٹراؤزر"),
    PAJAMA("Churidar / Pajama", "پاجامہ"),
    DHOTI("Dhoti Shalwar", "دھوتی شلوار")
}

@Keep
@IgnoreExtraProperties
data class ShalwarKameezMeasurement(
    val unit: String = MeasurementUnit.INCHES.code,
    // Kameez / Shirt Measurements (12 fields)
    val kameezLength: String = "",
    val shoulder: String = "",
    val chest: String = "",
    val kameezWaist: String = "",
    val kameezHip: String = "",
    val sleeveLength: String = "",
    val armhole: String = "",
    val neck: String = "",
    val cuff: String = "",
    val frontLength: String = "",
    val backLength: String = "",
    val collar: String = "",
    // Shalwar Measurements (7 fields)
    val shalwarLength: String = "",
    val shalwarWaist: String = "",
    val shalwarHip: String = "",
    val thigh: String = "",
    val knee: String = "",
    val bottomPaicha: String = "",
    val shalwarStyle: String = ShalwarStyleOption.SIMPLE.en
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "unit" to (if (unit == MeasurementUnit.CENTIMETERS.code) "cm" else "in"),
        "kameezLength" to kameezLength.trim().take(20),
        "shoulder" to shoulder.trim().take(20),
        "chest" to chest.trim().take(20),
        "kameezWaist" to kameezWaist.trim().take(20),
        "kameezHip" to kameezHip.trim().take(20),
        "sleeveLength" to sleeveLength.trim().take(20),
        "armhole" to armhole.trim().take(20),
        "neck" to neck.trim().take(20),
        "cuff" to cuff.trim().take(20),
        "frontLength" to frontLength.trim().take(20),
        "backLength" to backLength.trim().take(20),
        "collar" to collar.trim().take(20),
        "shalwarLength" to shalwarLength.trim().take(20),
        "shalwarWaist" to shalwarWaist.trim().take(20),
        "shalwarHip" to shalwarHip.trim().take(20),
        "thigh" to thigh.trim().take(20),
        "knee" to knee.trim().take(20),
        "bottomPaicha" to bottomPaicha.trim().take(20),
        "shalwarStyle" to shalwarStyle.trim().take(60)
    )

    fun hasAnyMeasurement(): Boolean {
        return listOf(
            kameezLength, shoulder, chest, kameezWaist, kameezHip,
            sleeveLength, armhole, neck, cuff, frontLength, backLength, collar,
            shalwarLength, shalwarWaist, shalwarHip, thigh, knee, bottomPaicha
        ).any { it.isNotBlank() }
    }

    fun convertedTo(targetUnit: MeasurementUnit): ShalwarKameezMeasurement {
        val current = MeasurementUnit.fromCode(unit)
        if (current == targetUnit) return this
        return copy(
            unit = targetUnit.code,
            kameezLength = convertValue(kameezLength, current, targetUnit),
            shoulder = convertValue(shoulder, current, targetUnit),
            chest = convertValue(chest, current, targetUnit),
            kameezWaist = convertValue(kameezWaist, current, targetUnit),
            kameezHip = convertValue(kameezHip, current, targetUnit),
            sleeveLength = convertValue(sleeveLength, current, targetUnit),
            armhole = convertValue(armhole, current, targetUnit),
            neck = convertValue(neck, current, targetUnit),
            cuff = convertValue(cuff, current, targetUnit),
            frontLength = convertValue(frontLength, current, targetUnit),
            backLength = convertValue(backLength, current, targetUnit),
            collar = convertValue(collar, current, targetUnit),
            shalwarLength = convertValue(shalwarLength, current, targetUnit),
            shalwarWaist = convertValue(shalwarWaist, current, targetUnit),
            shalwarHip = convertValue(shalwarHip, current, targetUnit),
            thigh = convertValue(thigh, current, targetUnit),
            knee = convertValue(knee, current, targetUnit),
            bottomPaicha = convertValue(bottomPaicha, current, targetUnit)
        )
    }

    companion object {
        private val formatter = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))

        fun convertValue(raw: String, from: MeasurementUnit, to: MeasurementUnit): String {
            if (from == to || raw.isBlank()) return raw
            val num = raw.trim().toDoubleOrNull() ?: return raw
            val converted = if (from == MeasurementUnit.INCHES && to == MeasurementUnit.CENTIMETERS) {
                num * 2.54
            } else {
                num / 2.54
            }
            return formatter.format(converted)
        }
    }
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerCode: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val measurementDate: String = "",
    val notes: String = "",
    val currentMeasurement: ShalwarKameezMeasurement = ShalwarKameezMeasurement(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MeasurementHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val measurementDate: String = "",
    val notes: String = "",
    val measurement: ShalwarKameezMeasurement = ShalwarKameezMeasurement(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShopProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val shopName: String = "Bespoke Tailor Shop",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val preferredUnit: String = MeasurementUnit.INCHES.code,
    val preferredLanguage: String = LanguageMode.BOTH.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
