package com.forgemind.data.mqtt

import com.forgemind.domain.model.Findings
import com.forgemind.domain.model.ResolvedAck
import com.forgemind.domain.service.MqttGateway
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Use this tonight. It doesn't talk to any broker -- call emitTestFindings()
 * from a button in the UI (or a test) to simulate the AI PC publishing a
 * findings packet, and the whole GenerateDiagnosisUseCase flow lights up.
 *
 * Tomorrow: replace with RealMqttGateway backed by an actual MQTT client
 * pointed at the AI PC broker. The interface doesn't change, so
 * AppContainer.kt is the only place that needs editing.
 */
class FakeMqttGateway : MqttGateway {

    private val incoming = MutableSharedFlow<Findings>(replay = 0, extraBufferCapacity = 8)
    private val published = mutableListOf<ResolvedAck>()

    override fun connect(brokerHost: String, port: Int) {
        // no-op: nothing to connect to
    }

    override fun observeFindings(machineId: String): Flow<Findings> =
        incoming.filter { it.machineId == machineId }

    override fun publishResolved(ack: ResolvedAck) {
        published += ack
        println("FakeMqttGateway: would publish to forgemind/machine/resolved -> $ack")
    }

    /** Call this from a "simulate anomaly" button in the demo UI. */
    suspend fun emitTestFindings(findings: Findings) {
        incoming.emit(findings)
    }

    fun publishedAcks(): List<ResolvedAck> = published.toList()
}
