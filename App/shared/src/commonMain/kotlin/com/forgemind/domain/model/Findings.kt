package com.forgemind.domain.model

import kotlinx.serialization.Serializable

/**
 * Telemetry snapshot from the Arduino UNO Q at the moment an anomaly fired.
 */
@Serializable
data class Telemetry(
    val tempC: Double,
    val currentA: Double,
    val rpm: Int
)

/**
 * Vision analysis result from the AI PC (NPU-accelerated model on the captured photo).
 */
@Serializable
data class VisionFindings(
    val bladeStatus: String,   // "bent" | "normal" | "cracked"
    val dustLevel: String      // "low" | "medium" | "high"
)

/**
 * Audio analysis result from the AI PC (spectral analysis on the captured clip).
 */
@Serializable
data class AudioFindings(
    val pattern: String,           // "periodic_scraping" | "continuous_grinding" | "normal"
    val spectralCentroidHz: Double,
    val periodic: Boolean
)

/**
 * The full fused findings packet published by the AI PC on
 * `forgemind/machine/findings`. This is what the phone app receives and
 * feeds to GenerateDiagnosisUseCase.
 */
@Serializable
data class Findings(
    val machineId: String,
    val vision: VisionFindings,
    val audio: AudioFindings,
    val telemetry: Telemetry,
    val timestamp: String
)
