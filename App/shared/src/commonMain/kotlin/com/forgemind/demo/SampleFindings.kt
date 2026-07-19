package com.forgemind.demo

import com.forgemind.domain.model.AudioFindings
import com.forgemind.domain.model.Findings
import com.forgemind.domain.model.Telemetry
import com.forgemind.domain.model.VisionFindings

/**
 * Matches the exact scenario used to validate the diagnosis logic:
 * bent blade, low dust, periodic scraping, mild temp rise.
 * Expected result: faultId == "bent_blade".
 */
object SampleFindings {

    val bentBlade = Findings(
        machineId = "FAN-01",
        vision = VisionFindings(bladeStatus = "bent", dustLevel = "low"),
        audio = AudioFindings(pattern = "periodic_scraping", spectralCentroidHz = 3400.0, periodic = true),
        telemetry = Telemetry(tempC = 47.2, currentA = 0.41, rpm = 2420),
        timestamp = "2026-07-18T14:32:11Z"
    )

    val dustBuildup = Findings(
        machineId = "FAN-01",
        vision = VisionFindings(bladeStatus = "normal", dustLevel = "high"),
        audio = AudioFindings(pattern = "normal", spectralCentroidHz = 1200.0, periodic = false),
        telemetry = Telemetry(tempC = 58.0, currentA = 0.38, rpm = 2100),
        timestamp = "2026-07-18T15:02:00Z"
    )

    val wornBearing = Findings(
        machineId = "FAN-01",
        vision = VisionFindings(bladeStatus = "normal", dustLevel = "low"),
        audio = AudioFindings(pattern = "continuous_grinding", spectralCentroidHz = 2100.0, periodic = false),
        telemetry = Telemetry(tempC = 61.0, currentA = 0.55, rpm = 1750),
        timestamp = "2026-07-18T15:10:00Z"
    )

    val motorOverload = Findings(
        machineId = "FAN-01",
        vision = VisionFindings(bladeStatus = "normal", dustLevel = "low"),
        audio = AudioFindings(pattern = "normal", spectralCentroidHz = 900.0, periodic = false),
        telemetry = Telemetry(tempC = 66.0, currentA = 0.92, rpm = 1400),
        timestamp = "2026-07-18T15:20:00Z"
    )
}
