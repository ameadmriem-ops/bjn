package com.example.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "TrustedSearch/1.0 (Android; OpenSearchEngine; contact@trustedsearch.org)")
                    .header("Accept", "application/json, text/html, */*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
