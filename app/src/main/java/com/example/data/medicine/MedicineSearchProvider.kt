package com.example.data.medicine

/**
 * Replaceable interface for medicine search providers.
 * Any online medicine database (NIH RxNorm, OpenFDA, Indian commercial providers like
 * Eka Care or DrugSetu) implements this interface.
 */
interface MedicineSearchProvider {
    /** Friendly name of the provider shown to users (e.g. "NIH RxNorm", "Indian Medicine Directory") */
    val providerName: String

    /** Whether this provider includes Indian brand names and formulations */
    val coversIndianMedicines: Boolean

    /** Whether this provider requires an external API key or commercial license */
    val requiresApiKey: Boolean

    /**
     * Searches for matching medicines given a search query string.
     * @param query User entered text (at least 2 characters)
     * @param limit Maximum results to return (default 8)
     */
    suspend fun search(query: String, limit: Int = 8): List<MedicineSearchResult>
}
