package com.example.data.medicine

import com.example.data.api.RxNormRetrofitApi
import com.example.data.api.RxNormService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Real online medicine database provider connected to the U.S. National Library
 * of Medicine (NLM / NIH) RxNorm Public REST API.
 *
 * Base URL: https://rxnav.nlm.nih.gov/REST/
 * Endpoints:
 *   - approximateTerm.json?term={query}&maxEntries={limit}
 *   - drugs.json?name={query}
 *
 * Access requirements: Public API, completely free, no API key required.
 * Coverage: Global generic medicines (Paracetamol, Amoxicillin, Metformin,
 *           Azithromycin, Omeprazole, Ciprofloxacin, etc.), clinical formulations,
 *           strengths, dosage forms, and major international brand names.
 */
class RxNormOnlineProvider(
    private val api: RxNormRetrofitApi = RxNormService.createDefaultApi()
) : MedicineSearchProvider {

    override val providerName: String = "NIH RxNorm Online Medicine Database"
    override val coversIndianMedicines: Boolean = false // Focuses on international clinical concepts and generic names
    override val requiresApiKey: Boolean = false

    override suspend fun search(query: String, limit: Int): List<MedicineSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val results = mutableListOf<MedicineSearchResult>()
        val seenKeys = mutableSetOf<String>()

        // 1. Try approximate terms match (handles prefix queries & approximate matches like "para", "amox", "metfor")
        try {
            val approxResponse = api.getApproximateTerms(cleanQuery, maxEntries = limit * 2)
            val candidates = approxResponse.approximateGroup?.candidate.orEmpty()

            for (candidate in candidates) {
                val name = candidate.name?.trim().orEmpty()
                val rxcui = candidate.rxcui?.trim().orEmpty()
                if (name.isBlank() || name.equals("null", ignoreCase = true)) continue

                val (brand, generic, strength, form) = parseMedicineConceptName(name)
                val dedupeKey = "${brand.lowercase(Locale.US)}_${strength.lowercase(Locale.US)}_${form.lowercase(Locale.US)}"
                if (seenKeys.add(dedupeKey)) {
                    results.add(
                        MedicineSearchResult(
                            id = rxcui.ifBlank { "rxnorm_${results.size}" },
                            brandName = brand,
                            genericName = generic,
                            strength = strength,
                            dosageForm = form,
                            source = MedicineSource.ONLINE_DATABASE,
                            isIndianBrand = false,
                            manufacturerOrLabeler = "RxNorm (RxCUI: $rxcui)",
                            extraDescription = name
                        )
                    )
                }
                if (results.size >= limit) break
            }
        } catch (e: Exception) {
            // Logged or handled by repository
        }

        // 2. If approximate returned fewer than limit, also check drugs.json (SBD / SCD concepts)
        if (results.size < limit) {
            try {
                val drugsResponse = api.getDrugProducts(cleanQuery)
                val conceptGroups = drugsResponse.drugGroup?.conceptGroup.orEmpty()
                for (group in conceptGroups) {
                    val tty = group.tty ?: ""
                    if (tty == "SBD" || tty == "SCD" || tty == "BPCK") {
                        val properties = group.conceptProperties.orEmpty()
                        for (prop in properties) {
                            val name = prop.name?.trim().orEmpty()
                            val rxcui = prop.rxcui?.trim().orEmpty()
                            if (name.isBlank()) continue

                            val (brand, generic, strength, form) = parseMedicineConceptName(name)
                            val dedupeKey = "${brand.lowercase(Locale.US)}_${strength.lowercase(Locale.US)}_${form.lowercase(Locale.US)}"
                            if (seenKeys.add(dedupeKey)) {
                                results.add(
                                    MedicineSearchResult(
                                        id = rxcui.ifBlank { "rxnorm_${results.size}" },
                                        brandName = if (prop.synonym?.isNotBlank() == true) prop.synonym else brand,
                                        genericName = if (tty == "SBD") brand else generic,
                                        strength = strength,
                                        dosageForm = form,
                                        source = MedicineSource.ONLINE_DATABASE,
                                        isIndianBrand = false,
                                        manufacturerOrLabeler = if (tty == "SBD") "Brand Product" else "Generic Clinical Concept",
                                        extraDescription = name
                                    )
                                )
                            }
                            if (results.size >= limit) break
                        }
                    }
                    if (results.size >= limit) break
                }
            } catch (_: Exception) {
                // Ignore secondary failure
            }
        }

        return@withContext results.take(limit)
    }

    companion object {
        /**
         * Parses a clinical concept name (e.g. "Paracetamol 500 MG Oral Tablet" or
         * "atorvastatin 20 MG Oral Tablet [Lipitor]") into (brandName, genericName, strength, formFactor).
         */
        fun parseMedicineConceptName(rawName: String): Quadruple<String, String, String, String> {
            var workingName = rawName.trim()

            // 1. Check for brand bracket, e.g. "atorvastatin 20 MG Oral Tablet [Lipitor]"
            val bracketMatch = Regex("""\[(.*?)\]""").find(workingName)
            val extractedBrand = bracketMatch?.groupValues?.getOrNull(1)?.trim()

            // 2. Extract dosage / strength snippet, e.g. "500 MG", "10 MG/ML", "625 MG", "250 MCG", "5 %"
            val strengthRegex = Regex("""(\d+(\.\d+)?\s*(MG|MCG|G|ML|IU|%|MG/ML))""", RegexOption.IGNORE_CASE)
            val strengthMatch = strengthRegex.find(workingName)?.value?.trim().orEmpty()

            // 3. Extract dosage form
            val upper = workingName.uppercase(Locale.US)
            val form = when {
                upper.contains("CAPSULE") -> "Capsule"
                upper.contains("TABLET") || upper.contains("CAPLET") -> "Tablet"
                upper.contains("INJECTION") || upper.contains("INJECTABLE") || upper.contains("PEN") -> "Injection"
                upper.contains("SUSPENSION") -> "Suspension"
                upper.contains("SOLUTION") || upper.contains("SYRUP") || upper.contains("LIQUID") -> "Liquid"
                upper.contains("INHALER") || upper.contains("INHALATION") || upper.contains("AEROSOL") -> "Inhaler"
                upper.contains("DROPS") || upper.contains("OPHTHALMIC") -> "Drops"
                upper.contains("CREAM") || upper.contains("OINTMENT") || upper.contains("GEL") -> "Topical"
                else -> "Tablet"
            }

            // 4. Extract base medicine / generic name
            var baseName = workingName
                .replace(Regex("""\[.*?\]"""), "") // remove bracket
                .replace(Regex("""\d+(\.\d+)?\s*(MG|MCG|G|ML|IU|%|MG/ML)""", RegexOption.IGNORE_CASE), "") // remove strength
                .replace(Regex("""(Oral Tablet|Oral Capsule|Oral Suspension|Oral Solution|Delayed Release|Extended Release|Film Coated|Tablet|Capsule|Injection|Solution)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .trim(',', '-', '/', ' ')

            // Capitalize first letter of each word
            baseName = baseName.split(" ").filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.lowercase(Locale.US).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                }

            val finalBrand = extractedBrand?.ifBlank { baseName } ?: baseName
            val finalGeneric = if (extractedBrand != null && extractedBrand.isNotBlank()) baseName else ""

            return Quadruple(
                first = finalBrand.ifBlank { rawName },
                second = finalGeneric,
                third = strengthMatch,
                fourth = form
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
