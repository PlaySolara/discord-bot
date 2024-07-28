package gg.solara.discord.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * @author GrowlyX
 * @since 7/28/2024
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class SolaraApplication

fun main(args: Array<String>)
{
    runApplication<SolaraApplication>(*args)
}

