package com.forgemind.domain.knowledge

/**
 * One entry from the maintenance manual, used as the retrieval unit for RAG.
 * Kept as plain Kotlin objects (not parsed from JSON) so there's zero
 * serialization setup needed to get the RAG demo running tonight.
 * Content lives in InMemoryKnowledgeBase.kt -- edit that file to add faults.
 */
data class KnowledgeEntry(
    val id: String,
    val title: String,
    val symptomKeywords: List<String>, // used for retrieval matching, see scoring in InMemoryKnowledgeBase
    val symptomsText: String,          // human-readable, goes into the LLM prompt
    val rootCause: String,
    val repairSteps: List<String>,
    val safety: List<String>,
    val tools: List<String>
)
