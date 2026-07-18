package com.forgemind.domain.service

/**
 * Hides the actual LLM backend from the use case layer. This is the single
 * most important abstraction for de-risking tonight/tomorrow:
 *
 * - Tonight: FakeTextGenerator (data/llm/FakeTextGenerator.kt) returns a
 *   templated answer built directly from the retrieved KnowledgeEntry, no
 *   model required. This lets you test and demo the ENTIRE app flow now.
 * - Tomorrow (fallback): a simple HTTP call to LM Studio's OpenAI-compatible
 *   local server running on the AI PC (no NPU export needed).
 * - Tomorrow (stretch, if time allows): swap in real on-device Gemma3 1B via
 *   Genie/NPU. Only this file changes -- GenerateDiagnosisUseCase does not.
 */
interface TextGenerator {
    suspend fun generate(prompt: String): String
}
