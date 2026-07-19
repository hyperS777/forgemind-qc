package com.forgemind.data.llm

import com.forgemind.domain.service.Clock
import kotlinx.datetime.Clock as KxClock

class SystemClock : Clock {
    override fun nowIso(): String = KxClock.System.now().toString()
}
