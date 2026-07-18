package com.forgemind.domain.usecase

import com.forgemind.domain.model.Findings
import com.forgemind.domain.service.MqttGateway
import kotlinx.coroutines.flow.Flow

class ObserveFindingsUseCase(
    private val mqttGateway: MqttGateway
) {
    operator fun invoke(machineId: String): Flow<Findings> =
        mqttGateway.observeFindings(machineId)
}
