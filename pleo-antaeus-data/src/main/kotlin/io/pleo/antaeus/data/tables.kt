/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status")
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
}

object ScheduledPaymentTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val invoiceId = reference("invoice_id", InvoiceTable.id)
    val scheduledTime = datetime("scheduled_time")
    val attempt = integer("attempt")
    val status = text("status")
    val lastStartedAt = datetime("last_started_at").nullable()
    val byStatusIndex = index("fetch_by_status", false, status)
}
