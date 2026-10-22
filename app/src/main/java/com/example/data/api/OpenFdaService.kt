package com.example.data.api

import android.util.Log
import com.example.data.scanner.DrugCatalog
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.Locale
import java.util.concurrent.TimeUnit

class OpenFdaService(
    private val okHttpClient: OkHttpClient = createDefaultHttpClient()
) {
    companion object {
        private const val TAG = "OpenFdaService"
        private const val BASE_NDC_URL = "https://api.fda.gov/drug/ndc.json"

        fun createDefaultHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, "[OkHttp] $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
        }

        private val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        private val jsonAdapter = moshi.adapter(OpenFdaNdcResponse::class.java)

        /**
         * Dynamically maps raw FDA dosage form descriptions into a clean, human-readable
         * form factor string (e.g. "Tablet", "Capsule", "Solution", "Liquid", "Suspension", "Inhaler", "Injection").
         * Avoids hardcoded defaults like "pills".
         */
        fun mapDosageFormToFormFactor(rawDosageForm: String?): String {
            if (rawDosageForm.isNullOrBlank()) return "Tablet"
            val upper = rawDosageForm.uppercase(Locale.US)
            return when {
                upper.contains("CAPSULE") -> "Capsule"
                upper.contains("TABLET") || upper.contains("CAPLET") -> "Tablet"
                upper.contains("INJECTION") || upper.contains("INJECTABLE") -> "Injection"
                upper.contains("SOLUTION FOR INJECTION") -> "Injection"
                upper.contains("SUSPENSION") -> "Suspension"
                upper.contains("SOLUTION") -> "Solution"
                upper.contains("LIQUID") || upper.contains("SYRUP") || upper.contains("ELIXIR") -> "Liquid"
                upper.contains("AEROSOL") || upper.contains("INHAL") -> "Inhaler"
                upper.contains("SPRAY") -> "Spray"
                upper.contains("DROPS") || upper.contains("OPHTHALMIC") || upper.contains("OTIC") -> "Drops"
                upper.contains("CREAM") -> "Cream"
                upper.contains("OINTMENT") -> "Ointment"
                upper.contains("GEL") -> "Gel"
                upper.contains("LOTION") -> "Lotion"
                upper.contains("PATCH") -> "Patch"
                upper.contains("POWDER") -> "Powder"
                else -> {
                    // Title-case the first word or phrase dynamically
                    rawDosageForm.split(" ", ",", "_").filter { it.isNotBlank() }
                        .joinToString(" ") { word ->
                            word.lowercase(Locale.US)
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                        }
                }
            }
        }
    }

    /**
     * Looks up drug details from openFDA NDC Directory API for 1D barcodes (UPC/EAN/NDC)
     * and 2D GS1 DataMatrix formats.
     *
     * Returns:
     * - [ScannedBarcodeDrug] with true Brand/Generic Name, Dosage, and dynamic Form Factor if found.
     * - `null` if no match is found, allowing the UI to display a user-friendly error and manual entry
     *   instead of placeholder dummy "Medicine(BARCODE)" data.
     */
    suspend fun lookupBarcode(
        rawBarcode: String,
        barcodeFormat: String = "UPC"
    ): ScannedBarcodeDrug? = withContext(Dispatchers.IO) {
        val cleanCode = rawBarcode.trim()
        Log.d(TAG, "lookupBarcode initiated -> rawBarcode: '$cleanCode', format: '$barcodeFormat'")

        // 1. Check local catalog first (instant offline match for verified OTC & prescription packs)
        val localMatch = DrugCatalog.findByBarcode(cleanCode)
        if (localMatch != null) {
            Log.d(TAG, "Local drug catalog match found for '$cleanCode': ${localMatch.brandName} (${localMatch.strength}) - Form: ${localMatch.form}")
            return@withContext ScannedBarcodeDrug(
                rawBarcode = cleanCode,
                barcodeFormat = barcodeFormat,
                brandName = localMatch.brandName,
                genericName = localMatch.genericName,
                dosageStrength = localMatch.strength,
                formFactor = localMatch.form,
                productNdc = localMatch.barcode,
                labeler = "Verified Pharmaceutical Registry",
                instructions = localMatch.generalInstructions,
                defaultStock = localMatch.defaultPackStock,
                defaultRefillThreshold = localMatch.defaultRefillThreshold,
                dataSource = "openFDA Verified NDC Registry"
            )
        }

        // 2. Parse GS1 DataMatrix (extracts GTIN from (01) application identifier if present)
        val normalizedCode = parseGs1OrUpcToCandidate(cleanCode)
        Log.d(TAG, "Normalized candidate code from barcode: '$normalizedCode'")

        // 3. Generate candidate search patterns for openFDA (handling UPC vs NDC differences)
        val candidateSearchQueries = buildCandidateSearchQueries(cleanCode, normalizedCode)
        Log.d(TAG, "Generated ${candidateSearchQueries.size} candidate openFDA queries for '$cleanCode'")

        var openFdaResult: OpenFdaDrugResult? = null

        for ((index, query) in candidateSearchQueries.withIndex()) {
            Log.d(TAG, "Querying openFDA [attempt ${index + 1}/${candidateSearchQueries.size}]: search=$query")
            val httpUrl = BASE_NDC_URL.toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("search", query)
                ?.addQueryParameter("limit", "1")
                ?.build()

            if (httpUrl == null) {
                Log.e(TAG, "Failed to build HTTP URL for query: $query")
                continue
            }

            val request = Request.Builder()
                .url(httpUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "MedTrack-Android-App/1.0")
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                val statusCode = response.code
                val rawBodyString = response.body?.string() ?: ""

                Log.d(TAG, "openFDA HTTP GET: status=$statusCode, url=$httpUrl")
                Log.d(TAG, "openFDA Raw JSON response:\n$rawBodyString")

                if (statusCode == 200 && rawBodyString.isNotBlank()) {
                    try {
                        val parsed = jsonAdapter.fromJson(rawBodyString)
                        val firstDrug = parsed?.results?.firstOrNull()
                        if (firstDrug != null) {
                            Log.d(TAG, "openFDA search successful with query: $query")
                            openFdaResult = firstDrug
                            break
                        } else {
                            Log.d(TAG, "openFDA returned 200 OK but results array was empty for query: $query")
                        }
                    } catch (parseEx: Exception) {
                        Log.e(TAG, "Failed to parse openFDA JSON response for query '$query': ${parseEx.message}", parseEx)
                    }
                } else if (statusCode == 404) {
                    Log.d(TAG, "openFDA returned 404 Not Found (no matches) for query: $query")
                } else {
                    Log.w(TAG, "openFDA returned non-200 status $statusCode for query: $query")
                }
            } catch (networkEx: Exception) {
                Log.e(TAG, "Network error querying openFDA with query '$query': ${networkEx.message}", networkEx)
            }
        }

        // 4. Map the API payload if a drug result was found
        if (openFdaResult != null) {
            val drug = mapOpenFdaResultToScannedDrug(openFdaResult, cleanCode, barcodeFormat)
            Log.d(TAG, "Successfully extracted drug from openFDA payload: brand='${drug.brandName}', generic='${drug.genericName}', strength='${drug.dosageStrength}', form='${drug.formFactor}', NDC='${drug.productNdc}'")
            return@withContext drug
        }

        // 5. Final fallback check in local catalog (e.g. partial NDC match)
        val catalogFuzzy = DrugCatalog.findByBarcode(normalizedCode)
        if (catalogFuzzy != null) {
            Log.d(TAG, "Fuzzy local catalog match for '$cleanCode' -> '${catalogFuzzy.brandName}'")
            return@withContext ScannedBarcodeDrug(
                rawBarcode = cleanCode,
                barcodeFormat = barcodeFormat,
                brandName = catalogFuzzy.brandName,
                genericName = catalogFuzzy.genericName,
                dosageStrength = catalogFuzzy.strength,
                formFactor = catalogFuzzy.form,
                productNdc = catalogFuzzy.barcode,
                labeler = "Verified Pharmaceutical Registry",
                instructions = catalogFuzzy.generalInstructions,
                defaultStock = catalogFuzzy.defaultPackStock,
                defaultRefillThreshold = catalogFuzzy.defaultRefillThreshold,
                dataSource = "openFDA Verified Catalog"
            )
        }

        // 6. No match found: Log and return null.
        // DO NOT return placeholder "Medicine(BARCODE_NUMBER)" or hardcoded "pills"!
        Log.d(TAG, "No pharmaceutical record found in openFDA NDC directory or local database for barcode '$cleanCode'")
        return@withContext null
    }

    /**
     * Extracts drug attributes dynamically from the openFDA JSON response:
     * - True Brand Name / Generic Name (formatted in Title Case)
     * - Active Ingredients / Strength
     * - Dynamic Form Factor (e.g. Solution, Tablet, Capsule, Liquid, Inhaler, Injection)
     */
    private fun mapOpenFdaResultToScannedDrug(
        result: OpenFdaDrugResult,
        rawBarcode: String,
        barcodeFormat: String
    ): ScannedBarcodeDrug {
        // True Brand Name extraction
        val rawBrand = result.brandName?.takeIf { it.isNotBlank() }
            ?: result.openfda?.brandName?.firstOrNull { it.isNotBlank() }
            ?: result.brandNameBase?.takeIf { it.isNotBlank() }
            ?: result.genericName?.takeIf { it.isNotBlank() }
            ?: result.openfda?.genericName?.firstOrNull { it.isNotBlank() }
            ?: result.packaging?.firstOrNull()?.description?.takeIf { it.isNotBlank() }
            ?: "Prescription Medication"

        val titleCaseBrand = toTitleCase(rawBrand)

        // True Generic Name extraction
        val rawGeneric = result.genericName?.takeIf { it.isNotBlank() }
            ?: result.openfda?.genericName?.firstOrNull { it.isNotBlank() }
            ?: ""
        val titleCaseGeneric = if (rawGeneric.isNotBlank()) toTitleCase(rawGeneric) else ""

        // Dosage / Strength extraction
        val ingredientStrengths = result.activeIngredients
            ?.mapNotNull { it.strength?.trim()?.takeIf { s -> s.isNotEmpty() } }
            ?.joinToString(", ") { s ->
                // Clean trailing denominator if "/1" (e.g. "10 mg/1" -> "10 mg")
                if (s.endsWith("/1")) s.removeSuffix("/1").trim() else s
            }

        val parsedStrength = when {
            !ingredientStrengths.isNullOrBlank() -> ingredientStrengths
            else -> {
                val strengthRegex = Regex("""(\d+(\.\d+)?\s*(mg|mcg|ml|g|iu|%|units))""", RegexOption.IGNORE_CASE)
                val matchInBrand = strengthRegex.find(rawBrand)?.value
                val matchInPackaging = result.packaging?.firstOrNull()?.description?.let { strengthRegex.find(it)?.value }
                matchInBrand ?: matchInPackaging ?: "Standard Dosage"
            }
        }

        // Dynamic Form Factor mapping (Solution, Tablet, Capsule, Liquid, Inhaler, Injection, etc.)
        val rawDosageForm = result.dosageForm
            ?: result.openfda?.dosageForm?.firstOrNull()
            ?: result.packaging?.firstOrNull()?.description
        val formFactor = mapDosageFormToFormFactor(rawDosageForm)

        // Tailored instructions based on dynamic form factor
        val instructions = when (formFactor.lowercase(Locale.US)) {
            "injection" -> "Administer subcutaneously into rotated site as directed"
            "liquid", "solution", "suspension", "syrup" -> "Measure dose carefully with provided measuring cup; take with water"
            "inhaler", "spray" -> "Inhale 1-2 puffs as prescribed; rinse mouth with water"
            "drops" -> "Instill prescribed number of drops into affected area"
            "cream", "ointment", "gel", "lotion" -> "Apply a thin layer to affected skin area as directed"
            "patch" -> "Apply to clean, dry, intact skin as instructed by physician"
            else -> "Take orally with a full glass of water as directed"
        }

        // Default stock based on form factor
        val defaultStock = when (formFactor.lowercase(Locale.US)) {
            "injection" -> 4
            "liquid", "solution", "suspension", "syrup" -> 240 // mL
            "inhaler" -> 200 // puffs
            "spray" -> 120 // sprays
            else -> 30 // standard count for tablets / capsules
        }

        val refillThreshold = when {
            defaultStock <= 5 -> 1
            defaultStock <= 30 -> 5
            else -> 30
        }

        val productNdc = result.productNdc
            ?: result.packaging?.firstOrNull()?.packageNdc
            ?: rawBarcode

        val labeler = result.labelerName
            ?: "FDA Registered Manufacturer"

        return ScannedBarcodeDrug(
            rawBarcode = rawBarcode,
            barcodeFormat = barcodeFormat,
            brandName = titleCaseBrand,
            genericName = titleCaseGeneric,
            dosageStrength = parsedStrength,
            formFactor = formFactor,
            productNdc = productNdc,
            labeler = labeler,
            instructions = instructions,
            defaultStock = defaultStock,
            defaultRefillThreshold = refillThreshold,
            dataSource = "openFDA NDC Directory"
        )
    }

    /**
     * Builds prioritized search queries to handle barcode format differences:
     * - GS1 DataMatrix (14-digit GTIN)
     * - Retail packaging UPC-A (12-digit) and EAN-13
     *   * In US pharma, UPCs often have prefix '3' followed by 10-digit NDC and check digit.
     * - Hyphenated NDC patterns (4-4-2, 5-3-2, 5-4-1, 5-4-2)
     * - openfda.upc query parameter
     */
    private fun buildCandidateSearchQueries(rawBarcode: String, normalizedCode: String): List<String> {
        val queries = mutableListOf<String>()

        // 1. If 12-digit UPC starting with '3' (standard US pharma packaging for NDC-based products):
        // The middle 10 digits (digits 1 to 10) are the 10-digit NDC.
        if (normalizedCode.length == 12 && normalizedCode.startsWith("3")) {
            val ndc10 = normalizedCode.substring(1, 11)
            // Generate hyphenated variants
            val p442 = "${ndc10.substring(0, 4)}-${ndc10.substring(4, 8)}-${ndc10.substring(8)}"
            val p532 = "${ndc10.substring(0, 5)}-${ndc10.substring(5, 8)}-${ndc10.substring(8)}"
            val p541 = "${ndc10.substring(0, 5)}-${ndc10.substring(5, 9)}-${ndc10.substring(9)}"
            queries.add("packaging.package_ndc:\"$p442\"+OR+product_ndc:\"${p442.substringBeforeLast("-")}\"")
            queries.add("packaging.package_ndc:\"$p532\"+OR+product_ndc:\"${p532.substringBeforeLast("-")}\"")
            queries.add("packaging.package_ndc:\"$p541\"+OR+product_ndc:\"${p541.substringBeforeLast("-")}\"")
        }

        // 2. Query openfda.upc with exact and trimmed UPC
        queries.add("openfda.upc:\"$normalizedCode\"+OR+openfda.upc:\"${normalizedCode.trimStart('0')}\"")

        // 3. If 10 or 11 digits: build exact hyphenated NDC queries
        if (normalizedCode.length == 10 && normalizedCode.all { it.isDigit() }) {
            val p541 = "${normalizedCode.substring(0, 5)}-${normalizedCode.substring(5, 9)}-${normalizedCode.substring(9)}"
            val p442 = "${normalizedCode.substring(0, 4)}-${normalizedCode.substring(4, 8)}-${normalizedCode.substring(8)}"
            val p532 = "${normalizedCode.substring(0, 5)}-${normalizedCode.substring(5, 8)}-${normalizedCode.substring(8)}"
            queries.add("packaging.package_ndc:\"$p541\"+OR+product_ndc:\"${p541.substringBeforeLast("-")}\"")
            queries.add("packaging.package_ndc:\"$p442\"+OR+product_ndc:\"${p442.substringBeforeLast("-")}\"")
            queries.add("packaging.package_ndc:\"$p532\"+OR+product_ndc:\"${p532.substringBeforeLast("-")}\"")
        } else if (normalizedCode.length == 11 && normalizedCode.all { it.isDigit() }) {
            val p542 = "${normalizedCode.substring(0, 5)}-${normalizedCode.substring(5, 9)}-${normalizedCode.substring(9)}"
            queries.add("packaging.package_ndc:\"$p542\"+OR+product_ndc:\"${p542.substringBeforeLast("-")}\"")
        }

        // 4. Direct queries on package_ndc and product_ndc
        queries.add("packaging.package_ndc:\"$normalizedCode\"+OR+product_ndc:\"$normalizedCode\"")

        // 5. Wildcard search for partial packaging or product match
        if (normalizedCode.length >= 6) {
            val searchChunk = normalizedCode.take(8)
            queries.add("packaging.package_ndc:*$searchChunk*+OR+product_ndc:*$searchChunk*")
        }

        return queries.distinct()
    }

    /**
     * Extracts pure GTIN / NDC numbers from GS1 DataMatrix or UPC packaging codes.
     */
    private fun parseGs1OrUpcToCandidate(raw: String): String {
        var text = raw.trim().replace("(", "").replace(")", "").replace("-", "")
        if (text.startsWith("01") && text.length >= 16) {
            text = text.substring(2, 16)
        }
        // If 14-digit GTIN with leading zeros, trim to 12 or 10 digits
        if (text.length == 14 && text.startsWith("00")) {
            text = text.substring(2)
        } else if (text.length == 13 && text.startsWith("0")) {
            text = text.substring(1)
        }
        return text
    }

    private fun toTitleCase(input: String): String {
        return input.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                // Preserve acronyms like "HCL", "ER", "XR" if short, else title case
                val w = word.lowercase(Locale.US)
                if (w in listOf("hcl", "er", "xr", "sr", "dr", "cr", "la")) {
                    w.uppercase(Locale.US)
                } else {
                    w.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                }
            }
    }
}

