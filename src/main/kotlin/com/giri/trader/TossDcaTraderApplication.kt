package com.giri.trader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TossDcaTraderApplication

fun main(args: Array<String>) {
    runApplication<TossDcaTraderApplication>(*args)
}
