package io.pleo.antaeus.core.services

import java.util.concurrent.Executor

abstract class AbstractQueueWorker<MESSAGE>(val threadCount: Int, val pollWaitTimeSecs: Int, val executor: Executor) {

    abstract fun process(message: MESSAGE): Boolean

    abstract fun markDone(message: MESSAGE)

    abstract fun markFailed(message: MESSAGE)

    abstract fun poll(): MESSAGE?

    fun start() {

        for (thread in 1..threadCount) {
            executor.execute(Worker())
            Thread.sleep(100)
        }
    }

    inner class Worker : Runnable {

        override fun run() {

            while(true) {

                val message: MESSAGE? = poll()

                if (message == null) {
                    Thread.sleep(pollWaitTimeSecs * 1000L)
                    println("nothing to process. Sleeping")
                    continue
                }

                try {
                    if (process(message)) {
                        markDone(message)
                    } else {
                        markFailed(message)
                    }
                } catch (e : Exception) {
                    markFailed(message)
                }
                Thread.sleep(500)
            }
        }
    }
}
