package com.forgemind.di

import com.forgemind.data.knowledge.InMemoryKnowledgeBase
import com.forgemind.data.llm.FakeTextGenerator
import com.forgemind.data.llm.SystemClock
import com.forgemind.data.mqtt.FakeMqttGateway
import com.forgemind.domain.knowledge.KnowledgeBase
import com.forgemind.domain.service.Clock
import com.forgemind.domain.service.MqttGateway
import com.forgemind.domain.service.TextGenerator
import com.forgemind.domain.usecase.GenerateDiagnosisUseCase
import com.forgemind.domain.usecase.ObserveFindingsUseCase
import com.forgemind.domain.usecase.PublishResolutionUseCase

/**
 * ============================================================
 *  TONIGHT vs TOMORROW -- read this before changing anything
 * ============================================================
 *
 * Tonight: everything below is wired to fakes. Run the app, tap the
 * "Simulate anomaly" button in the demo UI, and the whole findings ->
 * retrieval -> diagnosis -> display flow works end to end with zero
 * hardware and zero network calls.
 *
 * Tomorrow, in this exact order:
 *   1. Swap `mqttGateway` below for a RealMqttGateway once the AI PC's
 *      Mosquitto broker is up and the topic contract is confirmed live
 *      (see docs/MQTT_CONTRACT.md).
 *   2. Swap `textGenerator` for LmStudioTextGenerator (HTTP call to the AI
 *      PC) as the safe fallback. Only reach for an on-device NPU generator
 *      if that's solid AND there's time left -- it is the riskiest piece.
 *   3. Nothing in domain/usecase or presentation/ should need to change.
 *      If it does, the abstraction was leaky -- fix the interface, not the UI.
 */
object AppContainer {

    val clock: Clock = SystemClock()

    val knowledgeBase: KnowledgeBase = InMemoryKnowledgeBase()

    // TODO tomorrow: replace with RealMqttGateway(brokerHost = "<AI PC IP>")
    val mqttGateway: MqttGateway = FakeMqttGateway()

    // TODO tomorrow: replace with LmStudioTextGenerator(baseUrl = "http://<AI PC IP>:1234")
    val textGenerator: TextGenerator = FakeTextGenerator()

    val generateDiagnosisUseCase = GenerateDiagnosisUseCase(knowledgeBase, textGenerator)
    val observeFindingsUseCase = ObserveFindingsUseCase(mqttGateway)
    val publishResolutionUseCase = PublishResolutionUseCase(mqttGateway, clock)
}
