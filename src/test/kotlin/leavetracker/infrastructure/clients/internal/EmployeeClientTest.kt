package leavetracker.infrastructure.clients.internal

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import leavetracker.business.domain.Employee
import leavetracker.fixtures.stubNonSuccessfulGetEmployeeResponse
import leavetracker.fixtures.stubSuccessfulGetEmployeeResponse
import leavetracker.infrastructure.clients.HttpCallNonSucceededException
import leavetracker.infrastructure.defaultObjectMapper
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

@Tag("integration")
class EmployeeClientTest {

    private val employeeHttpService = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }

    private val okHttp = OkHttpClient.Builder().build()

    private val employeeClient = EmployeeClient(
        okHttpClient = okHttp,
        objectMapper = defaultObjectMapper,
        baseUrl = "http://localhost:${employeeHttpService.port()}"
    )

    @Test
    fun `should get an employee`() {
        val employeeId = UUID.randomUUID()
        employeeHttpService.stubSuccessfulGetEmployeeResponse(employeeId)

        val employee = employeeClient.get(employeeId)

        employee shouldBe Employee(employeeId, "DE")
    }

    @Test
    fun `should throw exception when get employee API call fails`() {
        val employeeId = UUID.randomUUID()
        employeeHttpService.stubNonSuccessfulGetEmployeeResponse(employeeId)

        val exception = shouldThrow<HttpCallNonSucceededException> { employeeClient.get(employeeId) }

        exception shouldBe HttpCallNonSucceededException("EmployeeClient", "employee does not exists", 404)
    }

}