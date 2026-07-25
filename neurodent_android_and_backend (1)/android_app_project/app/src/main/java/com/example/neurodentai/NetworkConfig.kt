package com.example.neurodentai

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkConfig {
    /**
     * The base URL for the FastAPI backend.
     * When running locally, use your local network IP (e.g., "http://10.170.241.109:8000")
     * When deployed to the cloud, replace this with your Render public HTTPS URL:
     * e.g., "https://neurodentai-backend.onrender.com"
     */
    const val BASE_URL = "https://oral-backend-api.onrender.com"

    // High timeout client to support Render's Free Tier container wake-up (takes 30-50s)
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}
