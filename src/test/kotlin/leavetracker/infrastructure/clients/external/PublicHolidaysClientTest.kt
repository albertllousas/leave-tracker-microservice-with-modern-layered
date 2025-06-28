package leavetracker.infrastructure.clients.external

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import leavetracker.fixtures.stubNonSuccessfulNagerPublicHolidaysResponse
import leavetracker.fixtures.stubSuccessfulNagerPublicHolidaysResponse
import leavetracker.infrastructure.clients.HttpCallNonSucceededException
import leavetracker.infrastructure.defaultObjectMapper
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.MonthDay
import java.time.Year

@Tag("integration")
class PublicHolidaysClientTest {

    private val nager = WireMockRule(wireMockConfig().dynamicPort()).also { it.start() }

    private val okHttp = OkHttpClient.Builder().build()

    private val publicHolidaysClient = PublicHolidaysClient(
        okHttpClient = okHttp,
        objectMapper = defaultObjectMapper,
        nagerBaseUrl = "http://localhost:${nager.port()}/v3"
    )

    @Test
    fun `should get public holidays for a given year and country code`() {
        nager.stubSuccessfulNagerPublicHolidaysResponse()

        val holidays = publicHolidaysClient.get(Year.of(2023), "DE")

        holidays shouldBe listOf(MonthDay.of(1, 1), MonthDay.of(12, 25))
    }

    @Test
    fun `should throw exception when the Nager API call fails`() {
        nager.stubNonSuccessfulNagerPublicHolidaysResponse()

        val exception = shouldThrow<HttpCallNonSucceededException> {
            publicHolidaysClient.get(Year.of(2023), "DE")
        }

        exception shouldBe HttpCallNonSucceededException("PublicHolidaysClient", "country does not exists", 404)
    }
}
