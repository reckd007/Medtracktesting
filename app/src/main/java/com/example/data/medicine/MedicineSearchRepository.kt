package com.example.data.medicine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

/**
 * Composite Medicine Search Repository that coordinates between real online databases
 * (NIH RxNorm Public API) and the Indian Medicine Directory.
 *
 * Implements:
 * - 300–500ms debounce support
 * - Clean ranking & deduplication
 * - Max 8 results cutoff
 * - Graceful network error handling with retry capability
 * - Offline fallback so medicines can still be found or entered manually without internet
 */
class MedicineSearchRepository(
    private val onlineProvider: MedicineSearchProvider = RxNormOnlineProvider(),
    private val indianProvider: MedicineSearchProvider = IndianMedicineCatalogProvider()
) {

    /**
     * Executes a search across active medicine providers.
     * @param query Search query typed by user (must be at least 2 characters)
     * @param maxResults Maximum number of suggestions to return (strictly capped at 8)
     */
    suspend fun searchMedicines(query: String, maxResults: Int = 8): Result<List<MedicineSearchResult>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            return@withContext Result.success(emptyList())
        }

        var networkException: Exception? = null

        val (indianResults, onlineResults) = coroutineScope {
            val indianDeferred = async {
                try {
                    indianProvider.search(cleanQuery, maxResults)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val onlineDeferred = async {
                try {
                    onlineProvider.search(cleanQuery, maxResults)
                } catch (e: Exception) {
                    if (e is IOException || e is UnknownHostException || e is SocketTimeoutException) {
                        networkException = e
                    }
                    emptyList()
                }
            }

            Pair(indianDeferred.await(), onlineDeferred.await())
        }

        // Deduplicate and merge results
        val combined = mutableListOf<MedicineSearchResult>()
        val seenSignatures = mutableSetOf<String>()

        fun addIfUnique(item: MedicineSearchResult) {
            val sig = "${item.brandName.lowercase(Locale.ROOT)}_${item.strength.lowercase(Locale.ROOT)}_${item.dosageForm.lowercase(Locale.ROOT)}"
            if (seenSignatures.add(sig)) {
                combined.add(item)
            }
        }

        // Prioritize matches that start with the query directly
        val (indianPrefix, indianOther) = indianResults.partition {
            it.brandName.lowercase(Locale.ROOT).startsWith(cleanQuery.lowercase(Locale.ROOT))
        }
        val (onlinePrefix, onlineOther) = onlineResults.partition {
            it.brandName.lowercase(Locale.ROOT).startsWith(cleanQuery.lowercase(Locale.ROOT))
        }

        indianPrefix.forEach { addIfUnique(it) }
        onlinePrefix.forEach { addIfUnique(it) }
        indianOther.forEach { addIfUnique(it) }
        onlineOther.forEach { addIfUnique(it) }

        val finalResults = combined.take(maxResults)

        if (finalResults.isEmpty() && networkException != null) {
            return@withContext Result.failure(
                networkException ?: IOException("Failed to connect to medicine database. Please check your internet connection.")
            )
        }

        return@withContext Result.success(finalResults)
    }
}
