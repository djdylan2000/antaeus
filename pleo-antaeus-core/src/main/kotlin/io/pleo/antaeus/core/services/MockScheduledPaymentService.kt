package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusDal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * This class uses the logic of ScheduledPaymentService except it schedules to the near future (15 secs)
 * instead of the 1st of the next month so that it is easy to see the scheduling and processing in action!
 */
class MockScheduledPaymentService(private val dal: AntaeusDal) : ScheduledPaymentService(dal) {

    override
    fun getScheduledDate(): Date {
        return Date(System.currentTimeMillis() + 15000L)
    }
}
