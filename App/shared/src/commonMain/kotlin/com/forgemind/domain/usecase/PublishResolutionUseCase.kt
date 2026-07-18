package com.forgemind.domain.usecase

import com.forgemind.domain.model.ResolvedAck
import com.forgemind.domain.service.Clock
import com.forgemind.domain.service.MqttGateway

class PublishResolutionUseCase(
    private val mqttGateway: MqttGateway,
    private val clock: Clock
) {
    fun invoke(machineId: String, faultId: String) {
        mqttGateway.publishResolved(
            ResolvedAck(
                machineId = machineId,
                resolved = true,
                faultType = faultId,
                timestamp = clock.nowIso()
            )
        )
    }
}
