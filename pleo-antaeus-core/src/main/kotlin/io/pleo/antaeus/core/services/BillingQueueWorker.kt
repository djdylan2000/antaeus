package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.models.ScheduledPayment
import java.util.concurrent.Executor


class BillingQueueWorker(
        threadCount: Int,
        pollWaitTimeSecs: Int,
        executor: Executor,
        private val scheduledPaymentService: ScheduledPaymentService,
        private val billingService: BillingService) : AbstractQueueWorker<ScheduledPayment>(threadCount, pollWaitTimeSecs, executor) {

    override fun process(scheduledPayment: ScheduledPayment): Boolean {
        try {
            return billingService.charge(scheduledPayment.invoiceId)
        } catch (e: CustomerNotFoundException) {
            // send an alert to the Account Manager
            markFailed(scheduledPayment)
        } catch (e: CurrencyMismatchException) {
            // send an alert to the customer
            markFailed(scheduledPayment)
        } catch (e: NetworkException) {
            // retry automatically
            markFailed(scheduledPayment)
        }
        return false
    }

    override fun markDone(scheduledPayment: ScheduledPayment) {
        scheduledPaymentService.markDone(scheduledPayment.id)
    }

    override fun markFailed(scheduledPayment: ScheduledPayment) {
        scheduledPaymentService.markFailed(scheduledPayment.id)
    }

    override fun poll(): ScheduledPayment? {
        return scheduledPaymentService.nextPending()
    }
}
