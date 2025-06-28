package leavetracker.business.services

import arrow.core.Either
import leavetracker.business.domain.AnnualLeave
import leavetracker.business.domain.LeaveRequested
import leavetracker.business.domain.LeaveType
import leavetracker.business.domain.RequestLeaveError
import leavetracker.infrastructure.clients.external.PublicHolidaysClient
import leavetracker.infrastructure.clients.internal.EmployeeClient
import leavetracker.infrastructure.db.AnnualLeaveRepository
import leavetracker.infrastructure.events.EventPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.MonthDay
import java.time.Year
import java.util.UUID

@Service
class RequestLeaveService(
    private val repository: AnnualLeaveRepository,
    private val employeeClient: EmployeeClient,
    private val publicHolidaysClient: PublicHolidaysClient,
    private val eventPublisher: EventPublisher,
    private val genId: () -> UUID = { UUID.randomUUID() },
    private val leaveDaysPerCountry: Map<String, Int>
) {

    @Transactional
    operator fun invoke(cmd: RequestLeaveCommand): Either<RequestLeaveError, Unit> {
        val employee = employeeClient.get(cmd.employeeId)
        val publicHolidays = publicHolidaysClient.get(cmd.year, employee.countryCode)
        val leave = repository.findBy(cmd.employeeId, cmd.year) ?: (AnnualLeave.create(genId(), cmd.employeeId, cmd.year))
        return leave.request(cmd.days, cmd.type, cmd.notes, employee, publicHolidays, leaveDaysPerCountry)
            .onRight { repository.save(it) }
            .onRight { eventPublisher.publish(LeaveRequested(it, cmd.days, cmd.type)) }
            .map { Unit }
    }
}

data class RequestLeaveCommand(val employeeId: UUID, val days: List<MonthDay>, val year: Year, val type: LeaveType, val notes: String?)
