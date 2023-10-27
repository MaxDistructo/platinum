package io.dedyn.engineermantra.omega.website

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebMain{
    fun main(args: Array<String>){
        runApplication<WebMain>(*args)
    }
}