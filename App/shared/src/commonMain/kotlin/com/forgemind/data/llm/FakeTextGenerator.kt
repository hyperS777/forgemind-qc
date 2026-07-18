package com.forgemind.data.llm

import com.forgemind.domain.service.TextGenerator

/**
 * Doesn't call any model. Just echoes back a JSON blob built from whatever
 * candidate the prompt lists first, so DiagnosisParser has something valid
 * to parse and the full app flow (findings -> retrieve -> "generate" ->
 * parse -> show on screen) is testable tonight with zero setup.
 *
 * The prompt already contains "[fault_id] Title" lines for each candidate --
 * this just regex-grabs the first bracketed id and wraps it as the expected
 * JSON shape. Swap for LmStudioTextGenerator or NpuTextGenerator tomorrow.
 */
class FakeTextGenerator : TextGenerator {
    override suspend fun generate(prompt: String): String {
        val firstId = Regex("""\[(\w+)]""").find(prompt)?.groupValues?.get(1) ?: "unknown"
        // repairSteps/safety/tools deliberately omitted (not empty arrays) so
        // DiagnosisParser falls back to the matched KnowledgeEntry's real
        // content instead of showing an empty checklist.
        return """
        {
          "faultId": "$firstId",
          "confidence": "medium",
          "explanation": "Fake generator: this is the top RAG match for the given findings. Swap FakeTextGenerator for a real LLM backend to get model-written reasoning here."
        }
        """.trimIndent()
    }
}
