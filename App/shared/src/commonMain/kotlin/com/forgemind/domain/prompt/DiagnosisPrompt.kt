package com.forgemind.domain.prompt

import com.forgemind.domain.knowledge.KnowledgeEntry
import com.forgemind.domain.model.Findings

/**
 * Builds the actual prompt sent to the LLM. This is the "reasoning trail"
 * piece that's ForgeMind's real differentiator -- it explicitly asks the
 * model to fuse three evidence types, not just look up one signal.
 */
object DiagnosisPrompt {

    fun build(findings: Findings, candidates: List<KnowledgeEntry>): String {
        val candidateBlock = candidates.joinToString("\n\n") { e ->
            """
            [${e.id}] ${e.title}
            Typical symptoms: ${e.symptomsText}
            Likely root cause: ${e.rootCause}
            """.trimIndent()
        }

        return """
        You are a maintenance copilot diagnosing a fault on machine ${findings.machineId}.

        Evidence collected:
        - Telemetry: temperature=${findings.telemetry.tempC}C, current=${findings.telemetry.currentA}A, rpm=${findings.telemetry.rpm}
        - Vision: blade_status=${findings.vision.bladeStatus}, dust_level=${findings.vision.dustLevel}
        - Audio: pattern=${findings.audio.pattern}, spectral_centroid=${findings.audio.spectralCentroidHz}Hz, periodic=${findings.audio.periodic}

        Candidate faults from the maintenance manual:
        $candidateBlock

        Task: pick the single most likely fault id from the candidates above, explain briefly
        which evidence supports it and which candidates it rules out, then return the repair
        steps, safety precautions, and required tools for that fault. Respond as compact JSON with
        keys: faultId, confidence (high/medium/low), explanation, repairSteps, safety, tools.
        """.trimIndent()
    }
}
