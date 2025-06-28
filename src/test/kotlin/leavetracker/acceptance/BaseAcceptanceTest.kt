package leavetracker.acceptance

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.restassured.RestAssured
import io.restassured.parsing.Parser
import leavetracker.fixtures.Postgres
import leavetracker.infrastructure.App
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import leavetracker.acceptance.BaseAcceptanceTest.Initializer

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [Initializer::class], classes = [App::class])
@DirtiesContext(classMode = AFTER_CLASS)
abstract class BaseAcceptanceTest {

    init {
        RestAssured.defaultParser = Parser.JSON
    }

    @LocalServerPort
    protected val servicePort: Int = 0

    companion object {
        val postgres: Postgres = Postgres()
        val wiremock = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.container.jdbcUrl,
                "spring.datasource.password=" + postgres.container.password,
                "spring.datasource.username=" + postgres.container.username,
                "spring.flyway.url=" + postgres.container.jdbcUrl,
                "spring.flyway.password=" + postgres.container.password,
                "spring.flyway.user=" + postgres.container.username,
                "clients.internal.employees.baseUrl=" + "http://localhost:${wiremock.port()}",
                "clients.external.nager.baseUrl=" + "http://localhost:${wiremock.port()}/v3",
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
