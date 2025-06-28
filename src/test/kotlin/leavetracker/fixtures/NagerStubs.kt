package leavetracker.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get

fun WireMockServer.stubSuccessfulNagerPublicHolidaysResponse(
    year: Int = 2023,
    countryCode: String = "DE"
) {
    stubFor(
        get("/v3/PublicHolidays/$year/$countryCode")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            [
                                {"date": "2023-01-01", "localName": "Neujahrstag", "name": "New Year's Day", "countryCode": "DE"},
                                {"date": "2023-12-25", "localName": "1. Weihnachtstag", "name": "Christmas Day", "countryCode": "DE"}
                            ]
                            """.trimIndent()
                    )
            )
    )
}

fun WireMockServer.stubNonSuccessfulNagerPublicHolidaysResponse(
    year: Int = 2023,
    countryCode: String = "DE",
    status: Int = 404,
    body: String = "country does not exists"
) {
    stubFor(get("/v3/PublicHolidays/$year/$countryCode").willReturn(aResponse().withStatus(status).withBody(body)))
}