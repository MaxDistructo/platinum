package io.dedyn.engineermantra.omega.shared

import java.time.Instant

/**
 * Create a new timer to be executed by the TimerThread
 *
 * This is constructed with the waitTime in seconds and the function to run at that time
 */
class Timer(val execute: () -> Unit, waitTime: Int) {
    /**
     * executionTime is used by the TimerThread to know when to actually execute
     * the code here.
     *
     * As such, we convert this to milliseconds for that, so we can compare to Instant.now()
     */
    val executionTime: Long
    init {
        executionTime = Instant.now().toEpochMilli() + (waitTime * 1000)
    }
}