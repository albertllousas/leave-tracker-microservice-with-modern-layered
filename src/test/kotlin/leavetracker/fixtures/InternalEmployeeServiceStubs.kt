package leavetracker.fixtures

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import java.util.UUID

fun WireMockServer.stubSuccessfulGetEmployeeResponse(
    employeeId : UUID = UUID.randomUUID(),
) {
    stubFor(
        WireMock.get("/employees/$employeeId")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(""" { "id": "$employeeId", "fullName": "John Doe", "countryCode": "DE" } """
                    )
            )
    )
}

fun WireMockServer.stubNonSuccessfulGetEmployeeResponse(
    employeeId : UUID = UUID.randomUUID(),
    status: Int = 404,
    body: String = "employee does not exists"
) {
    stubFor(WireMock.get("/employees/$employeeId").willReturn(aResponse().withStatus(status).withBody(body)))
}