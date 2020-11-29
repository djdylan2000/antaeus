package io.pleo.antaeus.models

import java.util.*

data class ScheduledPayment(
        val id: Int,
        val invoiceId: Int,
        val scheduledTime: Date,
        var attempt: Int = 0,
        var status: status,
        var lastStartedAt: Date
)

enum class status {
    SCHEDULED,
    SUCCESS,
    FAILURE
}
