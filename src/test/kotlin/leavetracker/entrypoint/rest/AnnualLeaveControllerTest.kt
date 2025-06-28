package leavetracker.entrypoint.rest

import arrow.core.left
import arrow.core.right
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import leavetracker.business.domain.LeaveType
import leavetracker.business.domain.RequestLeaveError
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.INVALID_COUNTRY
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.INVALID_DATES
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.MAX_LEAVE_DAYS_EXCEEDED
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.OVERLAPPING_LEAVE
import leavetracker.business.services.RequestLeaveCommand
import leavetracker.business.services.RequestLeaveService
import leavetracker.infrastructure.App
import leavetracker.infrastructure.FrameworkConfig
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.MonthDay
import java.time.Year
import java.util.UUID

@Tag("integration")
@WebMvcTest(AnnualLeaveController::class)
@ContextConfiguration(classes = [App::class, FrameworkConfig::class])
class AnnualLeaveControllerTest(@Autowired private val mvc: MockMvc) {

    @MockkBean
    private lateinit var requestLeave: RequestLeaveService

    @Test
    fun `should request an annual leave for a employee`() {
        val employeeId = UUID.randomUUID()
        every {
            requestLeave(
                RequestLeaveCommand(
                    employeeId = employeeId,
                    days = listOf(MonthDay.parse("--01-01"), MonthDay.parse("--02-01")),
                    year = Year.of(2023),
                    type = LeaveType.VACATION,
                    notes = "Family vacation"
                )
            )
        } returns Unit.right()

        val response = mvc.perform(
            post("/annual-leave/2023/$employeeId")
                .contentType("application/json")
                .content(""" { "days": ["01-01", "02-01"], "type": "VACATION", "notes": "Family vacation" } """)
        )

        response.andExpect(status().isNoContent)
    }

    @Test
    fun `should fail with 4XX when requesting an annual leave fails with a domain error`() {
        val employeeId = UUID.randomUUID()
        every { requestLeave(any()) } returns RequestLeaveError(MAX_LEAVE_DAYS_EXCEEDED).left()

        val response = mvc.perform(
            post("/annual-leave/2023/$employeeId")
                .contentType("application/json")
                .content(""" { "days": ["01-01", "02-01"], "type": "VACATION", "notes": "Family vacation" } """)
        )

        response.andExpect(status().is4xxClientError)
    }

    @TestFactory
    fun `should parse leave request errors`() = listOf(
        Pair(RequestLeaveError(INVALID_COUNTRY), 400),
        Pair(RequestLeaveError(INVALID_DATES), 400),
        Pair(RequestLeaveError(MAX_LEAVE_DAYS_EXCEEDED), 409),
        Pair(RequestLeaveError(OVERLAPPING_LEAVE), 409)
    ).map { (errorType, statusCode) ->
        DynamicTest.dynamicTest("should parse $errorType to http status code $statusCode") {
            AnnualLeaveController.asHttpError(errorType).statusCode.value() shouldBe statusCode
        }
    }
}
