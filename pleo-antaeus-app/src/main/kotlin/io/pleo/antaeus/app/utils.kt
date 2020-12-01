import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.BillingQueueWorker
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.ScheduledPaymentService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            val invoice = dal.createInvoice(
                    amount = Money(
                            value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                            currency = customer.currency
                    ),
                    customer = customer,
                    status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )

            // schedule for near future to test
            if (invoice?.status == InvoiceStatus.PENDING) {
                val scheduledDate = Date(System.currentTimeMillis() + 15000L + java.util.Random().nextInt(10_000))
                dal.createScheduledPayment(invoice!!.id, scheduledDate)
            }
        }
    }

}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            return Random.nextBoolean()
        }
    }
}

// mock Queue Worker to examine behavior
internal fun getBillingServiceQueueWorker(scheduledPaymentService: ScheduledPaymentService, billingService: BillingService): BillingQueueWorker {
    val executor = ThreadPoolExecutor(10, 10, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory())
    return BillingQueueWorker(1, 5, executor, scheduledPaymentService, billingService)
}