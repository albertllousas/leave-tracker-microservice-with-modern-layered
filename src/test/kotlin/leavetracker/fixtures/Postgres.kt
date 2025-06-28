package leavetracker.fixtures

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.postgresql.Driver
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import kotlin.also
import kotlin.apply
import java.time.Duration
import javax.sql.DataSource

class Postgres {

    val container: KtPostgreSQLContainer = KtPostgreSQLContainer()
        .withNetwork(Network.newNetwork())
        .withNetworkAliases("localhost")
        .withUsername("leavetracker")
        .withPassword("leavetracker")
        .withDatabaseName("leavetracker")
        .withStartupTimeout(Duration.ofSeconds(60))
        .waitingFor(Wait.forListeningPort())
        .also {
            it.start()
        }

    val datasource: DataSource = HikariDataSource().apply {
        driverClassName = Driver::class.qualifiedName
        jdbcUrl = container.jdbcUrl
        username = container.username
        password = container.password
    }.also {
        val flyway = Flyway(
            FluentConfiguration()
                .driver(Driver::class.qualifiedName)
                .dataSource(container.jdbcUrl, container.username, container.password)
                .cleanDisabled(false)
        )
        flyway.clean()
        flyway.migrate()
    }

    val jdbcTemplate: JdbcTemplate = JdbcTemplate(datasource)
}

class KtPostgreSQLContainer : PostgreSQLContainer<KtPostgreSQLContainer>("postgres:12")
