package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * -----------------------------------------------------------------------------------------
 * openFDA NDC Directory API Models
 * Official Base URL: https://api.fda.gov/drug/ndc.json
 * Public open registry without requiring API keys.
 * -----------------------------------------------------------------------------------------
 */

@JsonClass(generateAdapter = true)
data class OpenFdaNdcResponse(
    @Json(name = "meta")
    val meta: OpenFdaMeta? = null,
    @Json(name = "results")
    val results: List<OpenFdaDrugResult>? = null,
    @Json(name = "error")
    val error: OpenFdaError? = null
)

@JsonClass(generateAdapter = true)
data class OpenFdaMeta(
    @Json(name = "disclaimer")
    val disclaimer: String? = null,
    @Json(name = "terms")
    val terms: String? = null,
    @Json(name = "total")
    val total: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenFdaError(
    @Json(name = "code")
    val code: String? = null,
    @Json(name = "message")
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenFdaDrugResult(
    @Json(name = "product_id")
    val productId: String? = null,
    @Json(name = "product_ndc")
    val productNdc: String? = null,
    @Json(name = "brand_name")
    val brandName: String? = null,
    @Json(name = "generic_name")
    val genericName: String? = null,
    @Json(name = "brand_name_base")
    val brandNameBase: String? = null,
    @Json(name = "labeler_name")
    val labelerName: String? = null,
    @Json(name = "dosage_form")
    val dosageForm: String? = null,
    @Json(name = "route")
    val route: List<String>? = null,
    @Json(name = "active_ingredients")
    val activeIngredients: List<OpenFdaActiveIngredient>? = null,
    @Json(name = "packaging")
    val packaging: List<OpenFdaPackaging>? = null,
    @Json(name = "pharm_class")
    val pharmClass: List<String>? = null,
    @Json(name = "product_type")
    val productType: String? = null,
    @Json(name = "openfda")
    val openfda: OpenFdaDetails? = null
)

@JsonClass(generateAdapter = true)
data class OpenFdaDetails(
    @Json(name = "brand_name")
    val brandName: List<String>? = null,
    @Json(name = "generic_name")
    val genericName: List<String>? = null,
    @Json(name = "dosage_form")
    val dosageForm: List<String>? = null,
    @Json(name = "route")
    val route: List<String>? = null,
    @Json(name = "rxcui")
    val rxcui: List<String>? = null,
    @Json(name = "package_ndc")
    val packageNdc: List<String>? = null,
    @Json(name = "upc")
    val upc: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class OpenFdaActiveIngredient(
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "strength")
    val strength: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenFdaPackaging(
    @Json(name = "package_ndc")
    val packageNdc: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "sample")
    val sample: Boolean? = null
)

/**
 * Clean UI & Domain Model for Scanned Barcode Drug:
 * Extracted from openFDA / RxNorm or Local Catalog.
 */
data class ScannedBarcodeDrug(
    val rawBarcode: String,
    val barcodeFormat: String, // e.g., "UPC-A", "EAN-13", "GS1 DataMatrix", "NDC"
    val brandName: String,
    val genericName: String,
    val dosageStrength: String,
    val formFactor: String, // "Tablet", "Capsule", "Injection", "Liquid", "Inhaler"
    val productNdc: String,
    val labeler: String = "",
    val instructions: String = "Take as prescribed by physician",
    val defaultStock: Int = 30,
    val defaultRefillThreshold: Int = 5,
    val dataSource: String = "openFDA NDC Directory"
)
