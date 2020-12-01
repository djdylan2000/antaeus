/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

fun ResultRow.toInvoice(): Invoice = Invoice(
        id = this[InvoiceTable.id],
        amount = Money(
                value = this[InvoiceTable.value],
                currency = Currency.valueOf(this[InvoiceTable.currency])
        ),
        status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
        customerId = this[InvoiceTable.customerId]
)

fun ResultRow.toCustomer(): Customer = Customer(
        id = this[CustomerTable.id],
        currency = Currency.valueOf(this[CustomerTable.currency])
)

fun ResultRow.toScheduledPayment(): ScheduledPayment = ScheduledPayment(
        id = this[ScheduledPaymentTable.id],
        invoiceId = this[ScheduledPaymentTable.invoiceId],
        scheduledTime = this[ScheduledPaymentTable.scheduledTime].toDate(),
        attempt = this[ScheduledPaymentTable.attempt],
        lastStartedAt = this[ScheduledPaymentTable.lastStartedAt]?.toDate(),
        status = ScheduledPaymentStatus.valueOf(this[ScheduledPaymentTable.status])
)