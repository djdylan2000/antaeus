package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.ScheduledPayment
import io.pleo.antaeus.models.ScheduledPaymentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.*

open class ScheduledPaymentService(private val dal: AntaeusDal) {

    fun schedule(invoiceId: Int) {

        val scheduledDate = getScheduledDate()

        dal.createScheduledPayment(invoiceId, scheduledDate)
    }

    open fun getScheduledDate(): Date {
        val firstDayOfNextMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay()
        return Date.from(Instant.from(firstDayOfNextMonth.toInstant(ZoneOffset.UTC)))
    }

    fun nextPending(): ScheduledPayment? {
        return dal.pollNextScheduledPayment()
    }

    fun markDone(scheduledPaymentId: Int) {
        return dal.markScheduledPaymentSuccess(scheduledPaymentId)
    }

    fun markFailed(scheduledPaymentId: Int) {
        return dal.markScheduledFailed(scheduledPaymentId)
    }

}
