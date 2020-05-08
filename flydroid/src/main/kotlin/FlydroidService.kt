/*
 * gradle-avdl is a Gradle plugin to launch and stop Android
 * Virtual devices
 *
 * Copyright (C) 2020 by Frederic-Charles Barthelery.
 *
 * This file is part of gradle-avdl.
 *
 * gradle-avdl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * gradle-avdl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with gradle-avdl.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.geekorum.gradle.avdl.providers.flydroid

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.Duration

interface FlydroidService {
    @POST("start")
    suspend fun start(@Body startRequest: StartRequest): VirtualDevice

    @POST("stop")
    suspend fun stop(@Body stopRequest: StopRequest): VirtualDevice?

    @GET("devices/{deviceId}")
    suspend fun findVirtualDevice(@Path("deviceId") deviceId: String): Response<VirtualDevice?>
}

@OptIn(UnstableDefault::class)
fun createFlydroidService(serviceUrl: String, flydroidApiKey: String? = null) : FlydroidService {
    val client = OkHttpClient().newBuilder()
            // service may wait for the allocation before answering
            .readTimeout(Duration.ofMinutes(2))
            .apply {
                if (flydroidApiKey != null) {
                    addInterceptor {
                        val request = it.request().newBuilder()
                                .header("X-FLYDROID-KEY", flydroidApiKey)
                                .build()
                        it.proceed(request)
                    }
                }
            }.build()
    val retrofit = Retrofit.Builder()
            .client(client)
            .addConverterFactory(Json.Default.asConverterFactory("application/json".toMediaType()))
            .baseUrl(serviceUrl)
            .build()
    return retrofit.create(FlydroidService::class.java)
}


@Serializable
data class StartRequest(
        val name: String,
        val image: String,
        val adbkey: String
)

@Serializable
data class VirtualDevice(
        val id: String,
        val ip: String,
        val adbPort: Int,
        val consolePort: Int,
        val grpcPort: Int
)

@Serializable
data class StopRequest(
        val name: String
)
