/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.Date
import kotlin.random.Random

class AntaeusDal(private val db: Database) {

    val MAX_RETRY_ATTEMPTS = 5
    val TIMEOUT_MINS = 2

    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select { InvoiceTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .selectAll()
                    .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.customerId] = customer.id
                    } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                    .select { CustomerTable.id.eq(id) }
                    .firstOrNull()
                    ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                    .selectAll()
                    .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    private fun fetchScheduledPayment(id: Int): ScheduledPayment? {
        return transaction(db) {
            ScheduledPaymentTable
                    .select { ScheduledPaymentTable.id.eq(id) }
                    .firstOrNull()
                    ?.toScheduledPayment()
        }
    }

    fun createScheduledPayment(invoiceId: Int, scheduledTime: Date, paymentStatus: ScheduledPaymentStatus = ScheduledPaymentStatus.SCHEDULED): ScheduledPayment? {

        val id = transaction(db) {
            ScheduledPaymentTable.insert {
                it[this.invoiceId] = invoiceId
                it[this.scheduledTime] = DateTime(scheduledTime)
                it[this.attempt] = 0
                it[this.status] = paymentStatus.toString()
            } get ScheduledPaymentTable.id
        }

        return fetchScheduledPayment(id)
    }

    fun pollNextScheduledPayment(): ScheduledPayment? {

        var id: Int = transaction(db) transaction@{
            val next = ScheduledPaymentTable
                    .selectAll()
                    .forUpdate()
                    .andWhere { (ScheduledPaymentTable.scheduledTime.less(DateTime()) and (ScheduledPaymentTable.status neq ScheduledPaymentStatus.SUCCESS.toString())) }
                    .andWhere { ScheduledPaymentTable.lastStartedAt.isNull() or (ScheduledPaymentTable.attempt less MAX_RETRY_ATTEMPTS and (ScheduledPaymentTable.lastStartedAt.less(DateTime().minusMinutes(TIMEOUT_MINS))))}
                    .limit(1)
                    .firstOrNull()
                    ?.toScheduledPayment()
                    ?: return@transaction null

            ScheduledPaymentTable.update({ ScheduledPaymentTable.id.eq(next.id) }) {
                with(SqlExpressionBuilder) {
                    it.update(attempt, attempt + 1)
                    it[lastStartedAt] = DateTime()
                }
            }

            return@transaction next.id
        }
                ?: return null

        return fetchScheduledPayment(id)
    }

    fun markScheduledPaymentSuccess(id: Int) {

        transaction {
            // updating scheduled payment status and invoice status should be atomic

            ScheduledPaymentTable.update({ ScheduledPaymentTable.id.eq(id) }) {
                    it[status] = ScheduledPaymentStatus.SUCCESS.toString()
            }

            InvoiceTable.update({InvoiceTable.id.eq(fetchScheduledPayment(id)!!.invoiceId)})
            {
                it[status] = InvoiceStatus.PAID.toString()
            }
        }
    }

    fun markScheduledFailed(id: Int) {
        transaction {

            ScheduledPaymentTable.update({ ScheduledPaymentTable.id.eq(id) }) {
                it[status] = ScheduledPaymentStatus.FAILURE.toString()
            }

        }
    }

}
