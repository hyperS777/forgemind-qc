package com.forgemind.domain.service

import com.forgemind.domain.model.Findings
import com.forgemind.domain.model.ResolvedAck
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app needs from MQTT, hidden behind an interface so the
 * use cases never talk to a broker library directly.
 *
 * Tonight: use FakeMqttGateway (data/mqtt/FakeMqttGateway.kt) to test the
 * whole diagnosis flow without any broker running.
 *
 * Tomorrow: implement this with an Android MQTT client (e.g. HiveMQ MQTT
 * client or Eclipse Paho) pointed at the AI PC's Mosquitto broker, per the
 * topics in docs/MQTT_CONTRACT.md. Only this one file needs to change.
 */
interface MqttGateway {
    fun connect(brokerHost: String, port: Int = 1883)
    fun observeFindings(machineId: String): Flow<Findings>
    fun publishResolved(ack: ResolvedAck)
}
