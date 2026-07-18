package com.forgemind.android.model

import com.google.gson.annotations.SerializedName

data class TelemetrySnapshot(
    val temperature: Double,
    val current: Double,
    val rpm: Int,
    @SerializedName("anomaly_score") val anomalyScore: Double,
    @SerializedName("machine_id") val machineId: String? = null
)

data class NotificationEvent(
    val id: String,
    val title: String,
    val message: String,
    val severity: String,
    val timestamp: Double,
    val telemetry: TelemetrySnapshot?,
    val diagnosis: Diagnosis?
)

data class NotificationsResponse(
    val notifications: List<NotificationEvent>,
    val count: Int
)

data class AcknowledgeRequest(
    @SerializedName("notification_id") val notificationId: String
)
