package com.forgemind.data.knowledge

import com.forgemind.domain.knowledge.KnowledgeBase
import com.forgemind.domain.knowledge.KnowledgeEntry
import com.forgemind.domain.model.Findings

/**
 * Simplest RAG retrieval that works: turn the findings into a bag of keywords,
 * turn each manual entry into a bag of keywords, score by overlap, return the
 * best matches. No embeddings, no vector DB -- swap this out later for
 * something fancier only if you have time left after everything else works.
 */
class InMemoryKnowledgeBase : KnowledgeBase {

    private val entries: List<KnowledgeEntry> = buildEntries()

    override fun retrieve(findings: Findings, topN: Int): List<KnowledgeEntry> {
        val queryKeywords = findingsToKeywords(findings)
        return entries
            .map { entry -> entry to score(entry.symptomKeywords, queryKeywords) }
            .sortedByDescending { it.second }
            .take(topN)
            .map { it.first }
    }

    private fun score(entryKeywords: List<String>, queryKeywords: Set<String>): Int =
        entryKeywords.count { it in queryKeywords }

    private fun findingsToKeywords(f: Findings): Set<String> {
        val kws = mutableSetOf<String>()

        // vision
        when (f.vision.bladeStatus) {
            "bent" -> kws += listOf("bent_blade", "blade_deformed", "misaligned")
            "cracked" -> kws += listOf("cracked_blade", "blade_deformed")
        }
        when (f.vision.dustLevel) {
            "high", "medium" -> kws += listOf("dust", "debris")
        }

        // audio
        when (f.audio.pattern) {
            "periodic_scraping" -> kws += listOf("periodic", "scraping", "once_per_rotation")
            "continuous_grinding" -> kws += listOf("continuous", "grinding", "rumbling")
        }
        if (f.audio.spectralCentroidHz > 3000) kws += "high_frequency"

        // telemetry heuristics
        if (f.telemetry.currentA > 0.6) kws += listOf("current_spike", "overload")
        if (f.telemetry.tempC > 55) kws += "high_temp"
        if (f.telemetry.rpm < 1800) kws += "rpm_sag"

        return kws
    }

    private fun buildEntries(): List<KnowledgeEntry> = listOf(
        KnowledgeEntry(
            id = "bent_blade",
            title = "Bent Fan Blade / Blade-Housing Interference",
            symptomKeywords = listOf(
                "bent_blade", "blade_deformed", "misaligned",
                "periodic", "scraping", "once_per_rotation", "high_frequency"
            ),
            symptomsText = "Periodic scraping or clicking sound, once per rotation. " +
                "Elevated spectral centroid. Vision shows a visibly bent or misaligned blade. " +
                "Mild temperature rise, current slightly above nominal, RPM near normal.",
            rootCause = "One or more blades deformed, causing intermittent contact with the " +
                "grille or housing once per revolution.",
            repairSteps = listOf(
                "Power down and fully disconnect the fan from supply before touching it.",
                "Remove the protective grille (typically 4 corner screws or snap clips).",
                "Manually rotate the blade assembly by hand and confirm the contact point.",
                "Straighten the bent blade with padded pliers using small, even, incremental adjustments.",
                "Re-check clearance by spinning the blade by hand after each adjustment.",
                "Reassemble the grille and re-power. Reset the anomaly baseline after repair."
            ),
            safety = listOf(
                "Lockout power at the source before handling.",
                "Wear eye protection when applying pressure to blades in case of sudden slippage.",
                "Do not test-run a cracked blade even briefly."
            ),
            tools = listOf("Padded/needle-nose pliers", "Small screwdriver set", "Multimeter (optional)")
        ),
        KnowledgeEntry(
            id = "dust_buildup",
            title = "Dust / Debris Accumulation",
            symptomKeywords = listOf("dust", "debris", "rpm_sag", "high_temp"),
            symptomsText = "Gradual temperature rise, slightly reduced RPM, faint continuous " +
                "whooshing (not periodic scraping). Vision shows visible dust on blades/grille.",
            rootCause = "Accumulated dust adds mass and disrupts airflow, causing imbalance " +
                "and reduced cooling efficiency.",
            repairSteps = listOf(
                "Power down and disconnect before cleaning.",
                "Use compressed air or a soft brush to remove dust from blades and housing.",
                "Wipe down grille and motor housing with a dry cloth.",
                "Re-power and confirm temperature returns to baseline over a few minutes."
            ),
            safety = listOf(
                "Do not use liquid cleaners near the motor housing.",
                "Work in a ventilated area when using compressed air."
            ),
            tools = listOf("Compressed air can", "Soft brush", "Dry cloth")
        ),
        KnowledgeEntry(
            id = "worn_bearing",
            title = "Worn or Failing Bearing",
            symptomKeywords = listOf("continuous", "grinding", "rumbling", "rpm_sag", "current_spike", "high_temp"),
            symptomsText = "Continuous, non-periodic grinding or rumbling (not once-per-rotation). " +
                "Noticeable RPM sag under load. Temperature elevated more than expected. " +
                "Current may spike intermittently.",
            rootCause = "Bearing wear increases friction continuously through the rotation, " +
                "not at a single point per revolution.",
            repairSteps = listOf(
                "Power down and disconnect before inspection.",
                "Spin the shaft by hand and feel for grinding, roughness, or play in the bearing.",
                "If play or roughness is present, replace the bearing or the full motor assembly.",
                "After replacement, run at low load first and monitor temperature before full operation."
            ),
            safety = listOf(
                "Do not run a motor with a suspected failing bearing under full load -- risk of seizure.",
                "Allow the motor to cool fully before handling."
            ),
            tools = listOf("Bearing puller (if serviceable)", "Replacement bearing or motor assembly", "Multimeter")
        ),
        KnowledgeEntry(
            id = "loose_mount",
            title = "Loose Mounting / Vibration from Base",
            symptomKeywords = listOf("rpm_sag"), // deliberately weak match -- diagnosis of exclusion
            symptomsText = "Elevated vibration with no clear periodic audio signature. No blade " +
                "or dust abnormality. Temperature and current near nominal.",
            rootCause = "Mounting hardware has loosened, allowing the whole unit to vibrate " +
                "rather than an internal component fault.",
            repairSteps = listOf(
                "Power down before inspecting mounting points.",
                "Check and tighten all mounting screws/bolts.",
                "Confirm the mounting surface itself is stable and not resonating.",
                "Re-power and observe vibration score returns to baseline."
            ),
            safety = listOf("Support the unit before removing final mounting bolts to avoid drops."),
            tools = listOf("Screwdriver or wrench matching mount hardware")
        ),
        KnowledgeEntry(
            id = "motor_overload",
            title = "Motor Electrical Overload",
            symptomKeywords = listOf("current_spike", "overload", "rpm_sag", "high_temp"),
            symptomsText = "Sharp current spike well above nominal, RPM sagging or stalling, " +
                "rapid temperature rise. No periodic scraping. No blade deformity.",
            rootCause = "Electrical fault (winding short, locked rotor, or supply issue) " +
                "causing excess current draw.",
            repairSteps = listOf(
                "Power down immediately -- do not continue running an overloaded motor.",
                "Check for a physically locked rotor (obstruction preventing rotation).",
                "Measure winding resistance with a multimeter and compare to spec.",
                "If winding resistance is abnormal, replace the motor rather than repair."
            ),
            safety = listOf(
                "Disconnect power before any electrical measurement.",
                "Do not repeatedly power-cycle an overloaded motor -- risk of fire."
            ),
            tools = listOf("Multimeter", "Replacement motor (if winding fault confirmed)")
        )
    )
}
