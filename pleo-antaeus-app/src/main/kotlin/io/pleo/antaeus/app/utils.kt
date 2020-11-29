
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.AbstractQueueWorker
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.util.concurrent.*
import kotlin.math.abs
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
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
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
internal fun getQueueWorker(): AbstractQueueWorker<String> {
    val executor = ThreadPoolExecutor(10, 10, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory())
    return object: AbstractQueueWorker<String>(4, 5, executor) {

        override fun poll(): String? {
            return listOf("1", "2", "3", "4", "5", null).get(abs(Random.nextInt()) % 6)
        }

        override fun markDone(message: String) {
            println("processing message done " + message)
        }

        override fun process(message: String): Boolean {
            println("processing message " + message)

            // simulate processing
            Thread.sleep(500L + Random.nextInt(0, 300))
            return Random.nextBoolean()
        }
        override fun markFailed(message: String) {
            println("processing message failed " + message)
        }
    }
}