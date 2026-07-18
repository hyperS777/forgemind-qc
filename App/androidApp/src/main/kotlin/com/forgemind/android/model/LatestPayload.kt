package com.forgemind.android.model

import com.google.gson.annotations.SerializedName

data class LatestPayload(
    val image: String?,
    val audio: String?,
    val timestamp: Double,
    @SerializedName("anomaly_score") val anomalyScore: Double?,
    @SerializedName("machine_id") val machineId: String?,
    val telemetry: TelemetrySnapshot?
)
