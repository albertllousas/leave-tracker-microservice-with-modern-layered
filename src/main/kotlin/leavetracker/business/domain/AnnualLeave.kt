package leavetracker.business.domain

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import leavetracker.business.domain.AutoApproveError.AutoApproveErrorType.NO_PENDING_LEAVES
import leavetracker.business.domain.LeavePeriodStatus.APPROVED
import leavetracker.business.domain.LeavePeriodStatus.PENDING
import leavetracker.business.domain.LeaveType.SICK
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.INVALID_COUNTRY
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.INVALID_DATES
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.MAX_LEAVE_DAYS_EXCEEDED
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.OVERLAPPING_LEAVE
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.MonthDay
import java.time.Year
import java.util.UUID

data class AnnualLeave(
    val id: UUID,
    val employeeId: UUID,
    val year: Year,
    val leaves: List<LeavePeriod> = emptyList(),
    val version: Long = 0,
) {

    fun request(
        requestedDays: List<MonthDay>,
        type: LeaveType,
        notes: String?,
        employee: Employee,
        publicHolidays: List<MonthDay>,
        vacationDaysPerCountry: Map<String, Int>
    ): Either<RequestLeaveError, AnnualLeave> {
        val days = requestedDays.filterNot { it in publicHolidays }.filterNot { isWeekend(it, year) }
        val availableAnnualDays = vacationDaysPerCountry.getOrDefault(employee.countryCode, 0)
        val newLeavePeriod = LeavePeriod(days, type, if (type == SICK) APPROVED else PENDING, notes)
        return when {
            employee.countryCode !in vacationDaysPerCountry -> RequestLeaveError(INVALID_COUNTRY).left()
            availableAnnualDays < currentLeaveDays() + days.size -> RequestLeaveError(MAX_LEAVE_DAYS_EXCEEDED).left()
            days.isEmpty() -> RequestLeaveError(INVALID_DATES).left()
            leaves.any { it.days.intersect(days).isNotEmpty() } -> RequestLeaveError(OVERLAPPING_LEAVE).left()
            else -> copy(leaves = leaves + newLeavePeriod).right()
        }
    }

    fun currentLeaveDays(): Int = leaves.flatMap { it.days }.count()

    private fun isWeekend(monthDay: MonthDay, year: Year): Boolean {
        val dayOfWeek = monthDay.atYear(year.value).dayOfWeek
        return dayOfWeek == SATURDAY || dayOfWeek == SUNDAY
    }

    fun autoApprovePendingSickLeaves(): Either<AutoApproveError, AnnualLeave> {
        val pendingLeaves = leaves.filter { it.status == PENDING && it.type == SICK }
        return if (pendingLeaves.isEmpty()) AutoApproveError(NO_PENDING_LEAVES).left()
        else copy(leaves = leaves - pendingLeaves + pendingLeaves.map { it.copy(status = APPROVED) }).right()
    }

    companion object {
        fun create(newId: UUID, employeeId: UUID, year: Year): AnnualLeave = AnnualLeave(newId, employeeId, year)
    }
}

sealed interface DomainError

data class RequestLeaveError(val type: RequestLeaveErrorType) : DomainError {
    enum class RequestLeaveErrorType {
        INVALID_DATES, MAX_LEAVE_DAYS_EXCEEDED, OVERLAPPING_LEAVE, INVALID_COUNTRY,
    }
}

data class AutoApproveError(val type: AutoApproveErrorType) : DomainError {
    enum class AutoApproveErrorType {
        NO_PENDING_LEAVES, ANNUAL_LEAVE_NOT_FOUND,
    }
}


sealed class DomainEvent

data class LeaveRequested(val annualLeave: AnnualLeave, val days: List<MonthDay>, val type: LeaveType) : DomainEvent()

data class LeaveAutoApproved(val annualLeave: AnnualLeave) : DomainEvent()

data class LeavePeriod(
    val days: List<MonthDay>,
    val type: LeaveType,
    val status: LeavePeriodStatus,
    val notes: String? = null
)

enum class LeaveType {
    SICK, PARENTAL, UNPAID, VACATION, OTHER
}

enum class LeavePeriodStatus {
    PENDING, APPROVED, REJECTED, CANCELLED
}
