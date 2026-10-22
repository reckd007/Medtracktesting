package com.example.data.medicine

/**
 * Domain model representing a medicine match returned by online databases or local catalogs.
 */
data class MedicineSearchResult(
    val id: String,
    val brandName: String,
    val genericName: String = "",
    val strength: String = "",
    val dosageForm: String = "Tablet",
    val source: MedicineSource = MedicineSource.ONLINE_DATABASE,
    val isIndianBrand: Boolean = false,
    val manufacturerOrLabeler: String = "",
    val extraDescription: String = ""
) {
    /**
     * Clean display label showing brand name, generic name, strength, and form.
     * E.g. "Paracetamol 500 mg Tablet" or "Dolo 650 (Paracetamol) 650 mg Tablet"
     */
    val displayTitle: String
        get() = buildString {
            append(brandName)
            if (strength.isNotBlank() && !brandName.contains(strength, ignoreCase = true)) {
                append(" $strength")
            }
            if (dosageForm.isNotBlank() && !brandName.contains(dosageForm, ignoreCase = true)) {
                append(" $dosageForm")
            }
        }

    val subtitle: String
        get() = buildString {
            if (genericName.isNotBlank() && !genericName.equals(brandName, ignoreCase = true)) {
                append("Generic: $genericName")
            }
            if (manufacturerOrLabeler.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(manufacturerOrLabeler)
            }
            if (isEmpty() && strength.isNotBlank()) {
                append(strength)
                if (dosageForm.isNotBlank()) append(" • $dosageForm")
            }
        }
}

enum class MedicineSource {
    ONLINE_DATABASE,      // Official NIH RxNorm / OpenFDA Public REST API
    INDIAN_CATALOG,       // Verified Indian medicine database
    MANUAL_ENTRY          // Entered directly by user
}

sealed class MedicineSearchState {
    object Idle : MedicineSearchState()
    object Loading : MedicineSearchState()
    data class Success(val results: List<MedicineSearchResult>, val query: String) : MedicineSearchState()
    data class Empty(val query: String) : MedicineSearchState()
    data class Error(val message: String, val query: String, val canRetry: Boolean = true) : MedicineSearchState()
}
