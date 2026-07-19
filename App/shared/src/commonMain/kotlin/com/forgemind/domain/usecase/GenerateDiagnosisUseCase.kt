package com.forgemind.domain.usecase

import com.forgemind.domain.knowledge.KnowledgeBase
import com.forgemind.domain.model.Diagnosis
import com.forgemind.domain.model.Findings
import com.forgemind.domain.parse.DiagnosisParser
import com.forgemind.domain.prompt.DiagnosisPrompt
import com.forgemind.domain.service.TextGenerator

/**
 * findings -> retrieve candidates -> build prompt -> generate -> parse -> Diagnosis
 *
 * This is the whole RAG pipeline in one place. It doesn't know or care
 * whether TextGenerator is a fake, LM Studio over HTTP, or an on-device NPU
 * model -- swap the implementation, this class is untouched.
 */
class GenerateDiagnosisUseCase(
    private val knowledgeBase: KnowledgeBase,
    private val textGenerator: TextGenerator
) {
    suspend fun invoke(findings: Findings): Diagnosis {
        val candidates = knowledgeBase.retrieve(findings, topN = 3)
        require(candidates.isNotEmpty()) { "Knowledge base returned no candidates -- check keyword mapping" }

        val prompt = DiagnosisPrompt.build(findings, candidates)
        val rawResponse = textGenerator.generate(prompt)

        return DiagnosisParser.parse(rawResponse, topCandidate = candidates.first())
    }
}
