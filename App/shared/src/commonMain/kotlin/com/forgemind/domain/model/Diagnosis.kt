package com.forgemind.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Diagnosis(
    val faultId: String,           // matches a KnowledgeEntry.id, e.g. "bent_blade"
    val faultTitle: String,
    val confidence: String,        // "high" | "medium" | "low" -- kept simple for the demo
    val explanation: String,       // why the model thinks this, referencing the evidence
    val repairSteps: List<String>,
    val safety: List<String>,
    val tools: List<String>
)

@Serializable
data class ResolvedAck(
    val machineId: String,
    val resolved: Boolean = true,
    val faultType: String,
    val timestamp: String
)
