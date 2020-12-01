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

class ScheduledPaymentService(private val dal: AntaeusDal) {


    val testing = true

    fun schedule(invoiceId: Int) {

        val firstDayOfNextMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay()
        val scheduledDate: Date

        if (testing) {
            scheduledDate = Date(System.currentTimeMillis() + 15000L + Random().nextInt(10_000))
        } else {
            scheduledDate = Date.from(Instant.from(firstDayOfNextMonth.toInstant(ZoneOffset.UTC)))
        }

        dal.createScheduledPayment(invoiceId, scheduledDate)
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
