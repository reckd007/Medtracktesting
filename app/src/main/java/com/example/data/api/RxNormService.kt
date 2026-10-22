package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * NIH RxNorm Public REST API Interface
 * Official Base URL: https://rxnav.nlm.nih.gov/REST/
 * (No authentication or API keys required)
 */
interface RxNormRetrofitApi {

    /**
     * Feature 1: Spelling suggestions / search autocomplete
     * Endpoint: https://rxnav.nlm.nih.gov/REST/spellingsuggestions.json?name={name}
     */
    @GET("spellingsuggestions.json")
    suspend fun getSpellingSuggestions(
        @Query("name") name: String
    ): RxNormSpellingResponse

    /**
     * Feature 2: All drug products (strengths, forms, brands, and SCD/SBD concepts)
     * Endpoint: https://rxnav.nlm.nih.gov/REST/drugs.json?name={name}
     */
    @GET("drugs.json")
    suspend fun getDrugProducts(
        @Query("name") name: String
    ): RxNormDrugsResponse

    /**
     * Feature 3: Approximate term matches (as user types, returning concepts & candidates)
     * Endpoint: https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term={term}&maxEntries={maxEntries}
     */
    @GET("approximateTerm.json")
    suspend fun getApproximateTerms(
        @Query("term") term: String,
        @Query("maxEntries") maxEntries: Int = 8
    ): RxNormApproximateResponse
}

/**
 * Production-ready RxNorm Service with Coroutines, Flow Debounce (300ms),
 * robust error handling, and RxCUI concept group filtering.
 */
class RxNormService(
    private val api: RxNormRetrofitApi = createDefaultApi()
) {
    companion object {
        private const val BASE_URL = "https://rxnav.nlm.nih.gov/REST/"

        fun createDefaultApi(): RxNormRetrofitApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(RxNormRetrofitApi::class.java)
        }
    }

    /**
     * -------------------------------------------------------------------------------------
     * FEATURE 1: Real-time Autocomplete / Suggestion Search (Debounced 300ms)
     * -------------------------------------------------------------------------------------
     * Accepts a cold Flow of search queries (e.g. from a TextField).
     * Applies a 300ms debounce to prevent spamming the NIH endpoint on every keystroke.
     * Safely parses the nested JSON response and handles:
     *  - Blank or short inputs (< 2 chars) -> returns emptyList() immediately.
     *  - Empty results / no suggestions -> returns emptyList().
     *  - Network or parsing exceptions -> safely catches and emits emptyList().
     *
     * @param queryFlow Cold Flow emitting user search inputs as they type.
     * @return Flow emitting list of suggested drug names.
     */
    fun observeSpellingSuggestions(queryFlow: Flow<String>): Flow<List<String>> {
        return queryFlow
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(300L) // 300ms debounce window
            .flatMapLatest { query ->
                flow {
                    if (query.length < 2) {
                        emit(emptyList())
                        return@flow
                    }
                    try {
                        val suggestions = fetchSpellingSuggestions(query)
                        emit(suggestions)
                    } catch (e: Exception) {
                        // Edge case: Offline or network glitch - fall back gracefully
                        emit(emptyList())
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Direct suspend function for single-shot spelling suggestions.
     */
    suspend fun fetchSpellingSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        try {
            val response = api.getSpellingSuggestions(cleanQuery)
            // Parse: response -> suggestionGroup -> suggestionList -> suggestion (List<String>)
            val list = response.suggestionGroup?.suggestionList?.suggestion
            return@withContext list?.filter { it.isNotBlank() } ?: emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    /**
     * -------------------------------------------------------------------------------------
     * FEATURE 2: Fetching Strengths and Forms (RxCUI Mapping)
     * -------------------------------------------------------------------------------------
     * Queries the NIH RxNorm drug product endpoint `drugs.json?name={name}` and extracts
     * both SBD (Semantic Branded Drug) and SCD (Semantic Clinical Drug) concept groups.
     *
     * Concept Group Types:
     * - "SBD": Semantic Branded Drug (e.g., "Lipitor 10 MG Oral Tablet")
     * - "SCD": Semantic Clinical Drug (e.g., "atorvastatin 10 MG Oral Tablet")
     *
     * DATA FLOW EXPLANATION (From NIH API to Room Local Tracker Database):
     * 1. The user selects a drug name (e.g., "Lipitor") from the autocomplete suggestions.
     * 2. This method calls `api.getDrugProducts("Lipitor")`.
     * 3. We iterate over `response.drugGroup.conceptGroup`, filtering specifically for
     *    `tty == "SBD"` or `tty == "SCD"`.
     * 4. For each concept property, we extract:
     *    - `rxcui`: Unique RxNorm ID (e.g., "104895")
     *    - `name`: Clean clinical description (e.g., "Lipitor 10 MG Oral Tablet")
     *    - `synonym`: Optional brand alias
     * 5. We parse the strength/form (e.g., "10 MG", "Oral Tablet") and map to `RxNormDrugProduct`.
     * 6. The UI displays these in a dosage selection dropdown.
     * 7. When the user confirms, the ViewModel packages the selected `rxcui`, clean name,
     *    strength, and schedule into a `MedicationEntity(name = ..., dosage = ..., rxcui = ...)`
     *    and saves it permanently to the SQLite database via `MedicationDao.insertMedication()`.
     *
     * @param drugName The selected drug name from Feature 1.
     * @return Structured list of `RxNormDrugProduct` objects with unique `rxcui` and clean names.
     */
    suspend fun fetchDrugProducts(drugName: String): List<RxNormDrugProduct> = withContext(Dispatchers.IO) {
        val cleanName = drugName.trim()
        if (cleanName.isBlank()) return@withContext emptyList()

        try {
            val response = api.getDrugProducts(cleanName)
            val conceptGroups = response.drugGroup?.conceptGroup ?: return@withContext emptyList()

            val products = mutableListOf<RxNormDrugProduct>()

            // Filter for SBD (Semantic Branded Drug) and SCD (Semantic Clinical Drug)
            val targetTypes = setOf("SBD", "SCD")

            for (group in conceptGroups) {
                val tty = group.tty ?: continue
                if (targetTypes.contains(tty)) {
                    val properties = group.conceptProperties ?: continue
                    for (prop in properties) {
                        val rxcui = prop.rxcui ?: continue
                        val name = prop.name ?: continue

                        // Extract dosage snippet (e.g. "10 MG" from "Lipitor 10 MG Oral Tablet")
                        val dosageRegex = Regex("""(\d+(\.\d+)?\s*(MG|MCG|G|ML|IU|%|MG/ML))""", RegexOption.IGNORE_CASE)
                        val dosageMatch = dosageRegex.find(name)?.value ?: ""

                        // Extract form snippet (e.g. "Tablet", "Capsule", "Injection")
                        val form = when {
                            name.contains("Capsule", ignoreCase = true) -> "Capsule"
                            name.contains("Injection", ignoreCase = true) || name.contains("Pen", ignoreCase = true) -> "Injection"
                            name.contains("Solution", ignoreCase = true) || name.contains("Oral Suspension", ignoreCase = true) -> "Liquid"
                            name.contains("Inhaler", ignoreCase = true) || name.contains("Inhalation", ignoreCase = true) -> "Inhaler"
                            else -> "Tablet"
                        }

                        products.add(
                            RxNormDrugProduct(
                                rxcui = rxcui,
                                displayName = name,
                                synonym = prop.synonym,
                                termType = tty,
                                parsedDosage = dosageMatch,
                                parsedForm = form
                            )
                        )
                    }
                }
            }

            // Return unique by RxCUI, sorted with Brand names (SBD) first then generic (SCD)
            return@withContext products.distinctBy { it.rxcui }.sortedBy { it.termType != "SBD" }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
}
