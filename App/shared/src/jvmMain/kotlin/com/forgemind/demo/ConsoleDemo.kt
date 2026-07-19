package com.forgemind.demo

import com.forgemind.di.AppContainer
import kotlinx.coroutines.runBlocking

/**
 * Run tonight with:  ./gradlew :shared:run
 * (or right-click this file -> Run 'ConsoleDemoKt' in Android Studio/IntelliJ)
 *
 * Prints the diagnosis for all four sample scenarios using the fake MQTT
 * gateway and fake text generator -- confirms retrieval + prompt + parse
 * all work end to end before any hardware exists.
 */
fun main() = runBlocking {
    val scenarios = mapOf(
        "bent_blade (expected)" to SampleFindings.bentBlade,
        "dust_buildup (expected)" to SampleFindings.dustBuildup,
        "worn_bearing (expected)" to SampleFindings.wornBearing,
        "motor_overload (expected)" to SampleFindings.motorOverload
    )

    scenarios.forEach { (label, findings) ->
        println("=== $label ===")
        val diagnosis = AppContainer.generateDiagnosisUseCase.invoke(findings)
        println("Got: ${diagnosis.faultId} -- ${diagnosis.faultTitle}")
        println("Confidence: ${diagnosis.confidence}")
        println("Repair steps: ${diagnosis.repairSteps.size} steps loaded")
        println()
    }
}
