package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.ScheduledPayment

class ScheduledPaymentService(private val dal: AntaeusDal) {

    fun fetchAllPending(): List<ScheduledPayment> {
        return ArrayList()
    }

    fun nextPending(): ScheduledPayment {
        return dal.pollNextScheduledPayment();
    }

    fun done(scheduledPaymentId: Int) {
        return dal.markScheduledPaymentSuccess(scheduledPaymentId);
    }

}
