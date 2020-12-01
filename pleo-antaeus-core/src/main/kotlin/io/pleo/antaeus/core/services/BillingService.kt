package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(

    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun charge(invoiceId: Int): Boolean {
        val invoice = invoiceService.fetch(invoiceId)
        return paymentProvider.charge(invoice)
    }
}
