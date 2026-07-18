package com.forgemind.domain.knowledge

import com.forgemind.domain.model.Findings

interface KnowledgeBase {
    /**
     * Returns the top-N manual entries most relevant to the given findings,
     * ranked best-match first. This is the "R" (retrieval) in RAG.
     */
    fun retrieve(findings: Findings, topN: Int = 2): List<KnowledgeEntry>
}
