package leavetracker.fixtures

import leavetracker.business.domain.AnnualLeave
import leavetracker.business.domain.Employee
import leavetracker.business.domain.LeavePeriod
import leavetracker.business.domain.LeavePeriodStatus
import leavetracker.business.domain.LeaveType
import leavetracker.business.services.RequestLeaveCommand
import java.time.MonthDay
import java.time.Year
import java.util.UUID

object TestBuilders {

    fun buildRequestLeaveCommand(
        employeeId: UUID = UUID.randomUUID(),
        days: List<MonthDay> = listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)),
        year: Year = Year.of(2025),
        type: LeaveType = LeaveType.VACATION,
        notes: String? = null
    ) = RequestLeaveCommand(employeeId = employeeId, days = days, year = year, type = type, notes = notes)

    fun buildEmployee(
        id: UUID = UUID.randomUUID(),
        countryCode: String = "ES",
    ) = Employee(id = id, countryCode = countryCode)

    fun buildAnnualLeave(
        id: UUID = UUID.randomUUID(),
        year: Year = Year.of(2025),
        employeeId: UUID = UUID.randomUUID(),
        leaves: List<LeavePeriod> = emptyList(),
        version: Long = 0L
    ) = AnnualLeave(id = id, year = year, employeeId = employeeId, leaves = leaves, version = version)

    fun buildLeavePeriod(
        days: List<MonthDay> = listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)),
        status: LeavePeriodStatus = LeavePeriodStatus.PENDING,
        type: LeaveType = LeaveType.VACATION,
        notes: String? = null
    ) = LeavePeriod(days = days, type = type, status = status, notes = notes)
}
