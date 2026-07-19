package com.forgemind.android.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val PREFS_NAME = "forgemind_settings"
    private const val KEY_BASE_URL = "backend_url"
    // Default backend for local Wi-Fi. Replace if your PC IP differs.
    private const val DEFAULT_BASE_URL = "http://10.194.181.38:8000/"

    private var baseUrl = DEFAULT_BASE_URL
    private var apiService: ForgeMindApi? = null

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    fun initialize(context: Context) {
        val savedUrl = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)
            ?.trim()
            .orEmpty()

        baseUrl = normalizeBaseUrl(
            if (savedUrl.isNotEmpty()) savedUrl else DEFAULT_BASE_URL
        )

        android.util.Log.i("RetrofitClient", "Initialized with baseUrl=$baseUrl")

        apiService = buildApi()
    }

    fun setBaseUrl(url: String, context: Context) {
        val normalizedUrl = normalizeBaseUrl(url)
        baseUrl = normalizedUrl
        android.util.Log.i("RetrofitClient", "BaseUrl updated to $baseUrl")

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .apply()

        apiService = buildApi()
    }

    fun getBaseUrl(context: Context): String {
        if (apiService == null) {
            initialize(context)
        }
        return baseUrl
    }

    val api: ForgeMindApi
        get() = apiService ?: buildApi().also { apiService = it }

    private fun buildApi(): ForgeMindApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ForgeMindApi::class.java)
    }

    private fun normalizeBaseUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val withScheme = if (
            trimmed.startsWith("http://") || trimmed.startsWith("https://")
        ) {
            trimmed
        } else {
            "http://$trimmed"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }

}