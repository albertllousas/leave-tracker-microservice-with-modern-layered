package leavetracker.entrypoint.rest

import leavetracker.business.domain.LeaveType
import leavetracker.business.domain.RequestLeaveError
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.*
import leavetracker.business.services.RequestLeaveCommand
import leavetracker.business.services.RequestLeaveService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.MonthDay
import java.time.Year
import java.util.UUID

@RestController
@RequestMapping("/annual-leave/{year}/{employeeId}")
class AnnualLeaveController(private val requestLeave: RequestLeaveService) {

    @PostMapping
    fun request(@PathVariable year: Year, @PathVariable employeeId: UUID, @RequestBody request: RequestLeaveHttpDto) =
        requestLeave(RequestLeaveCommand(employeeId, request.days.asMonthDays(), year, request.type, request.notes))
            .fold(ifLeft = { asHttpError(it) }, ifRight = { ResponseEntity.noContent().build() })

    private fun List<String>.asMonthDays(): List<MonthDay> = this.map { MonthDay.parse("--$it") }

    companion object {
        fun asHttpError(error: RequestLeaveError): ResponseEntity<HttpErrorResponse> = when (error.type) {
            INVALID_COUNTRY, INVALID_DATES -> ResponseEntity.badRequest().body(HttpErrorResponse(error.type.name))
            MAX_LEAVE_DAYS_EXCEEDED, OVERLAPPING_LEAVE -> ResponseEntity.status(409)
                .body(HttpErrorResponse(error.type.name))
        }
    }
}

data class RequestLeaveHttpDto(val days: List<String>, val type: LeaveType, val notes: String?)

data class HttpErrorResponse(val error: String)
