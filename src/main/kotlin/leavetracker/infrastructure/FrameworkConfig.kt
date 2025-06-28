package leavetracker.infrastructure

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.MonthDay

@SpringBootApplication(scanBasePackages = ["leavetracker"])
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}

@Configuration
class FrameworkConfig {

    @Bean
    fun meterRegistry() = SimpleMeterRegistry()

    @Bean
    fun objectMapper(): ObjectMapper = defaultObjectMapper

    @Bean
    fun okHttpClient(): okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder().build()

    @Bean
    @ConfigurationProperties(prefix = "app.leave-days-per-country")
    fun leaveDaysPerCountry(): Map<String, Int> = mutableMapOf()
}

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(MonthDayModule())
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

class MonthDayModule : SimpleModule() {
    init {
        addDeserializer(MonthDay::class.java,
            object : JsonDeserializer<MonthDay>() {
                override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MonthDay =
                    MonthDay.parse("--${p.text.removePrefix("--")}")
            }
        )
    }
}
