package com.forgemind.domain.parse

import com.forgemind.domain.knowledge.KnowledgeEntry
import com.forgemind.domain.model.Diagnosis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object DiagnosisParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Tries to parse the LLM's JSON response. If the model returns anything
     * malformed (very common with small on-device models under time
     * pressure), fall back to the top-ranked candidate entry directly so the
     * demo never shows a broken screen -- worst case, the "explanation" is
     * generic instead of model-generated.
     */
    fun parse(rawResponse: String, topCandidate: KnowledgeEntry): Diagnosis {
        return runCatching {
            val obj = json.parseToJsonElement(extractJsonBlock(rawResponse)) as JsonObject
            Diagnosis(
                faultId = obj["faultId"]?.jsonPrimitive?.content ?: topCandidate.id,
                faultTitle = topCandidate.title,
                confidence = obj["confidence"]?.jsonPrimitive?.content ?: "medium",
                explanation = obj["explanation"]?.jsonPrimitive?.content ?: "",
                repairSteps = obj["repairSteps"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: topCandidate.repairSteps,
                safety = obj["safety"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: topCandidate.safety,
                tools = obj["tools"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: topCandidate.tools
            )
        }.getOrElse {
            fallback(topCandidate)
        }
    }

    private fun fallback(entry: KnowledgeEntry): Diagnosis = Diagnosis(
        faultId = entry.id,
        faultTitle = entry.title,
        confidence = "medium",
        explanation = "Matched based on manual retrieval: ${entry.rootCause}",
        repairSteps = entry.repairSteps,
        safety = entry.safety,
        tools = entry.tools
    )

    /** Small models often wrap JSON in prose or markdown fences -- strip that. */
    private fun extractJsonBlock(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) text.substring(start, end + 1) else text
    }
}
