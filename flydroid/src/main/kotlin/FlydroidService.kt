package com.geekorum.gradle.avdl.providers.flydroid

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.time.Duration

interface FlydroidService {
    @POST("start")
    suspend fun start(@Body startRequest: StartRequest): VirtualDevice

    @POST("stop")
    suspend fun stop(@Body stopRequest: StopRequest): VirtualDevice?
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
        val email: String,
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
