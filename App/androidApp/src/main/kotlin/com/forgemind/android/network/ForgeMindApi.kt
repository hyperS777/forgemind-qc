package com.forgemind.android.network

import com.forgemind.android.model.Diagnosis
import com.forgemind.android.model.LatestPayload
import com.forgemind.android.model.NotificationsResponse
import com.forgemind.android.model.AcknowledgeRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ForgeMindApi {

    @Multipart
    @POST("analyze")
    suspend fun analyze(

        @Part image: MultipartBody.Part?,

        @Part audio: MultipartBody.Part?,

        @Part("temperature") temperature: RequestBody,

        @Part("current") current: RequestBody,

        @Part("rpm") rpm: RequestBody,

        @Part("anomaly_score") anomalyScore: RequestBody

    ): Diagnosis

    @GET("latest-payload")
    suspend fun latestPayload(): LatestPayload

    @POST("acknowledge-payload")
    suspend fun acknowledgePayload()

    @GET("notifications")
    suspend fun notifications(): NotificationsResponse

    @POST("notifications/acknowledge")
    suspend fun acknowledgeNotification(@retrofit2.http.Body request: AcknowledgeRequest)

    @POST("notifications/clear")
    suspend fun clearNotifications()
}