package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * -----------------------------------------------------------------------------------------
 * NIH RxNorm API Models (Spelling Suggestions & Drug Concepts)
 * Base URL: https://rxnav.nlm.nih.gov/REST/
 * -----------------------------------------------------------------------------------------
 */

// --- Feature 1: Spelling Suggestions Models ---

@JsonClass(generateAdapter = true)
data class RxNormSpellingResponse(
    @Json(name = "suggestionGroup")
    val suggestionGroup: RxNormSuggestionGroup? = null
)

@JsonClass(generateAdapter = true)
data class RxNormSuggestionGroup(
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "suggestionList")
    val suggestionList: RxNormSuggestionList? = null
)

@JsonClass(generateAdapter = true)
data class RxNormSuggestionList(
    @Json(name = "suggestion")
    val suggestion: List<String>? = null
)

// --- Feature 2: Drug Products (SBD / SCD Concepts & RxCUI) Models ---

@JsonClass(generateAdapter = true)
data class RxNormDrugsResponse(
    @Json(name = "drugGroup")
    val drugGroup: RxNormDrugGroup? = null
)

@JsonClass(generateAdapter = true)
data class RxNormDrugGroup(
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "conceptGroup")
    val conceptGroup: List<RxNormConceptGroup>? = null
)

@JsonClass(generateAdapter = true)
data class RxNormConceptGroup(
    @Json(name = "tty")
    val tty: String? = null, // Term type e.g. "SBD" (Semantic Branded Drug) or "SCD" (Semantic Clinical Drug)
    @Json(name = "conceptProperties")
    val conceptProperties: List<RxNormConceptProperty>? = null
)

@JsonClass(generateAdapter = true)
data class RxNormConceptProperty(
    @Json(name = "rxcui")
    val rxcui: String? = null,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "synonym")
    val synonym: String? = null,
    @Json(name = "tty")
    val tty: String? = null,
    @Json(name = "language")
    val language: String? = null,
    @Json(name = "suppress")
    val suppress: String? = null,
    @Json(name = "umlscui")
    val umlscui: String? = null
)

// --- Feature 3: Approximate Term Candidates Models ---

@JsonClass(generateAdapter = true)
data class RxNormApproximateResponse(
    @Json(name = "approximateGroup")
    val approximateGroup: RxNormApproximateGroup? = null
)

@JsonClass(generateAdapter = true)
data class RxNormApproximateGroup(
    @Json(name = "inputTerm")
    val inputTerm: String? = null,
    @Json(name = "candidate")
    val candidate: List<RxNormCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class RxNormCandidate(
    @Json(name = "rxcui")
    val rxcui: String? = null,
    @Json(name = "rxaui")
    val rxaui: String? = null,
    @Json(name = "score")
    val score: String? = null,
    @Json(name = "rank")
    val rank: String? = null,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "source")
    val source: String? = null
)


/**
 * Clean UI Domain Model:
 * This model is exposed to ViewModels, Compose UIs, and local Room database entities.
 */
data class RxNormDrugProduct(
    val rxcui: String,
    val displayName: String,
    val synonym: String? = null,
    val termType: String = "SBD", // "SBD" (Brand) or "SCD" (Generic)
    val parsedDosage: String = "",
    val parsedForm: String = "Tablet"
)
