package io.dedyn.engineermantra.omega.shared

import java.time.Instant
import java.util.*

class TimerThread: Thread(){
    private val timers: LinkedList<Timer> = LinkedList()
    fun registerTimer(timer: Timer){
        timers.add(timer)
    }

    override fun run(){
        while(true)
        {
            val removeList: MutableList<Timer> = mutableListOf()
            for(timer in timers)
            {
                if(timer.executionTime >= Instant.now().toEpochMilli())
                {
                    timer.execute()
                    removeList.add(timer)
                }
            }
            removeList.forEach(timers::remove)
            sleep(10000)
        }
    }
}