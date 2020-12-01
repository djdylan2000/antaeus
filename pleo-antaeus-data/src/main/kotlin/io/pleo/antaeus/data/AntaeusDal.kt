/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import io.pleo.antaeus.models.Currency
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.*
import java.util.Date

class AntaeusDal(private val db: Database) {
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

    fun fetchScheduledPayment(id: Int): ScheduledPayment? {
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

        val id: Int = transaction(db) transaction@{
            addLogger(StdOutSqlLogger)

            val next = ScheduledPaymentTable
                    .selectAll()
                    .forUpdate()
                    .andWhere { (ScheduledPaymentTable.scheduledTime.less(CurrentDateTime()) and (ScheduledPaymentTable.status neq ScheduledPaymentStatus.SUCCESS.toString())) }
                    .andWhere { ScheduledPaymentTable.lastStartedAt.isNull() or (ScheduledPaymentTable.attempt less 5 and (ScheduledPaymentTable.lastStartedAt.less(DateTime().minusMinutes(5))))}
                    .firstOrNull()
                    ?.toScheduledPayment()
                    ?: return@transaction null

            ScheduledPaymentTable.update({ ScheduledPaymentTable.id.eq(next.id) }) {
                with(SqlExpressionBuilder) {
                    it.update(ScheduledPaymentTable.attempt, ScheduledPaymentTable.attempt + 1)
                    it.set(ScheduledPaymentTable.lastStartedAt, DateTime())
                }
            }
        }
                ?: return null

        return fetchScheduledPayment(id)

    }
}
