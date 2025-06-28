package leavetracker.acceptance

import io.restassured.RestAssured
import leavetracker.fixtures.stubSuccessfulGetEmployeeResponse
import leavetracker.fixtures.stubSuccessfulNagerPublicHolidaysResponse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID

@Tag("acceptance")
class RequestLeaveAcceptanceTest : BaseAcceptanceTest() {

    @Test
    fun `should request a leave`() {
        val employeeId = UUID.randomUUID()
        wiremock.stubSuccessfulGetEmployeeResponse(employeeId)
        wiremock.stubSuccessfulNagerPublicHolidaysResponse(2023)

        RestAssured
            .given()
            .body(""" { "days": ["01-01", "02-01"], "type": "VACATION", "notes": "Family vacation" } """)
            .contentType("application/json")
            .accept("application/json")
            .port(servicePort)
            .`when`()
            .post("/annual-leave/2023/$employeeId")
            .then()
            .log().all()
            .assertThat().statusCode(204)
    }
}
