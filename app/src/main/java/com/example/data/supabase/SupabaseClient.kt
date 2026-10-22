package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Lightweight, robust Supabase Client using PostgREST HTTP/REST semantics.
 * Connects directly to local Supabase instances (e.g. http://10.0.2.2:54321).
 */
class SupabaseClient(
    private val baseUrlProvider: () -> String = { SupabaseConfig.getSupabaseUrl() },
    private val anonKeyProvider: () -> String = { SupabaseConfig.getAnonKey() }
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun getRestUrl(path: String): String {
        val base = baseUrlProvider().trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$base/rest/v1/$cleanPath"
    }

    /**
     * Executes GET against a Supabase PostgREST table endpoint.
     */
    suspend fun get(tableOrQuery: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = getRestUrl(tableOrQuery)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Authorization", "Bearer ${anonKeyProvider()}")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Log.w(TAG, "Supabase GET failed [$url]: code=${response.code}, body=$body")
                Result.failure(Exception("Supabase GET error: HTTP ${response.code} - $body"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Supabase GET exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Executes POST to insert rows into a Supabase table.
     */
    suspend fun post(table: String, jsonPayload: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = getRestUrl(table)
            val requestBody = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Authorization", "Bearer ${anonKeyProvider()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Log.w(TAG, "Supabase POST failed [$url]: code=${response.code}, body=$body")
                Result.failure(Exception("Supabase POST error: HTTP ${response.code} - $body"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Supabase POST exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Executes PATCH to update rows in a Supabase table.
     */
    suspend fun patch(tableWithQuery: String, jsonPayload: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = getRestUrl(tableWithQuery)
            val requestBody = jsonPayload.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Authorization", "Bearer ${anonKeyProvider()}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Log.w(TAG, "Supabase PATCH failed [$url]: code=${response.code}, body=$body")
                Result.failure(Exception("Supabase PATCH error: HTTP ${response.code} - $body"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Supabase PATCH exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Executes Supabase Stored Procedure / RPC.
     */
    suspend fun rpc(functionName: String, jsonParams: String = "{}"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = getRestUrl("rpc/$functionName")
            val requestBody = jsonParams.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Authorization", "Bearer ${anonKeyProvider()}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Log.w(TAG, "Supabase RPC failed [$url]: code=${response.code}, body=$body")
                Result.failure(Exception("Supabase RPC error: HTTP ${response.code} - $body"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Supabase RPC exception: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SupabaseClient"
    }
}
