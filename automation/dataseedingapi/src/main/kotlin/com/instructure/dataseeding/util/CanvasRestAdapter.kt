/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.instructure.dataseeding.util

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object CanvasRestAdapter {

    val canvasDomain = "mobileqa.test.instructure.com"
    val baseUrl = "https://$canvasDomain/api/v1/"
    val redirectUri = "urn:ietf:wg:oauth:2.0:oob"
    val adminToken = DATA_SEEDING_ADMIN_TOKEN
    val clientId = DATA_SEEDING_CLIENT_ID
    val clientSecret = DATA_SEEDING_CLIENT_SECRET
    private val TIMEOUT_IN_SECONDS = 60L

    private val adminOkHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val authInterceptor = AuthRequestInterceptor(adminToken)

        OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(RestRetryInterceptor)
                .readTimeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build()
    }

    private fun okHttpClientWithToken(token: String): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val authInterceptor = AuthRequestInterceptor(token)

        return OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(RestRetryInterceptor)
                .readTimeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build()
    }

    private val noAuthOkHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(RestRetryInterceptor)
                .readTimeout(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .build()
    }

    val adminRetrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(adminOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    fun retrofitWithToken(token: String): Retrofit =
            Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClientWithToken(token))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    val noAuthRetrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(noAuthOkHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}
