package io.pleo.antaeus.core.services

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.ScheduledPayment
import io.pleo.antaeus.models.ScheduledPaymentStatus
import java.util.*
import kotlin.collections.ArrayList

class ScheduledPaymentService(private val dal: AntaeusDal) {

    fun fetchAllPending(): List<ScheduledPayment> {
        return ArrayList()
    }

    fun nextPending(): ScheduledPayment {
        return ScheduledPayment(1, 1, Date(),0, ScheduledPaymentStatus.SCHEDULED, Date())
//        return dal.pollNextScheduledPayment();
    }

    fun done(scheduledPaymentId: Int) {

//        return dal.markScheduledPaymentSuccess(scheduledPaymentId);
    }

}
