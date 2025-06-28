package leavetracker.business.domain

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import leavetracker.business.domain.AutoApproveError.AutoApproveErrorType.NO_PENDING_LEAVES
import leavetracker.business.domain.LeavePeriodStatus.APPROVED
import leavetracker.business.domain.LeavePeriodStatus.PENDING
import leavetracker.business.domain.LeaveType.VACATION
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.INVALID_COUNTRY
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.INVALID_DATES
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.MAX_LEAVE_DAYS_EXCEEDED
import leavetracker.business.domain.RequestLeaveError.RequestLeaveErrorType.OVERLAPPING_LEAVE
import leavetracker.fixtures.TestBuilders
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.MonthDay
import java.time.Year
import java.util.UUID

class AnnualLeaveTest {

    @Nested
    inner class Create {

        @Test
        fun `should create an annual leave`() {
            val newId = UUID.randomUUID()
            val employeeId = UUID.randomUUID()

            val result = AnnualLeave.create(newId, employeeId, Year.of(2023))

            result shouldBe AnnualLeave(newId, employeeId, Year.of(2023), emptyList(), version = 0)
        }
    }

    @Nested
    inner class RequestLeave {

        @Test
        fun `should successfully request a leave`() {
            val annualLeave = TestBuilders.buildAnnualLeave()
            val employee = TestBuilders.buildEmployee(annualLeave.employeeId)

            val result = annualLeave.request(
                requestedDays = listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)),
                type = VACATION,
                notes = "Trip to the mountains",
                employee = employee,
                publicHolidays = emptyList(),
                vacationDaysPerCountry = mapOf(employee.countryCode to 20)
            )

            result shouldBe annualLeave.copy(
                leaves = listOf(
                    LeavePeriod(
                        days = listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)),
                        type = VACATION,
                        status = PENDING,
                        notes = "Trip to the mountains"
                    )
                )
            ).right()
        }

        @Test
        fun `should successfully request a leave with public holidays and weekends filtered out`() {
            val annualLeave = TestBuilders.buildAnnualLeave(year = Year.of(2025))
            val employee = TestBuilders.buildEmployee(annualLeave.employeeId)

            val newYearsDay = MonthDay.of(1, 1)
            val saturday = MonthDay.of(1, 4) // Assuming this is a Saturday
            val vacationDaysPerCountry = mapOf(employee.countryCode to 20)

            val result = annualLeave.request(
                requestedDays = listOf(newYearsDay, MonthDay.of(1, 2), MonthDay.of(1, 3), saturday),
                type = VACATION,
                notes = "Trip to the mountains",
                employee = employee,
                publicHolidays = listOf(newYearsDay),
                vacationDaysPerCountry = vacationDaysPerCountry
            )

            result shouldBe annualLeave.copy(
                leaves = listOf(
                    LeavePeriod(
                        days = listOf(MonthDay.of(1, 2), MonthDay.of(1, 3)),
                        type = VACATION,
                        status = PENDING,
                        notes = "Trip to the mountains"
                    )
                )
            ).right()
        }

        @Test
        fun `should fail requesting a leave when requested days are empty`() {
            val annualLeave = TestBuilders.buildAnnualLeave()
            val employee = TestBuilders.buildEmployee(annualLeave.employeeId)

            val result = annualLeave.request(emptyList(), VACATION, null, employee, emptyList(), mapOf("ES" to 20))

            result shouldBe RequestLeaveError(INVALID_DATES).left()
        }

        @Test
        fun `should fail requesting a leave when requested country is invalid`() {
            val annualLeave = TestBuilders.buildAnnualLeave()
            val employee = TestBuilders.buildEmployee(annualLeave.employeeId, countryCode = "XX")

            val result =
                annualLeave.request(listOf(MonthDay.of(1, 1)), VACATION, null, employee, emptyList(), mapOf("ES" to 20))

            result shouldBe RequestLeaveError(INVALID_COUNTRY).left()
        }

        @Test
        fun `should fail requesting a leave when requested days exceed maximum allowed`() {
            val annualLeave = TestBuilders.buildAnnualLeave()
            val employee = TestBuilders.buildEmployee(annualLeave.employeeId)

            val result = annualLeave.request(
                listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)),
                VACATION,
                null,
                employee,
                emptyList(),
                mapOf("ES" to 1)
            )

            result shouldBe RequestLeaveError(MAX_LEAVE_DAYS_EXCEEDED).left()
        }

        @Test
        fun `should fail requesting a leave when requested days overlap with existing leaves`() {
            val existingLeave = TestBuilders.buildLeavePeriod(
                days = listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)),
                status = APPROVED,
                type = VACATION
            )
            val annualLeave = TestBuilders.buildAnnualLeave(leaves = listOf(existingLeave))
            val employee = TestBuilders.buildEmployee(annualLeave.employeeId)

            val result = annualLeave.request(
                listOf(MonthDay.of(1, 2), MonthDay.of(1, 3)),
                VACATION,
                null,
                employee,
                emptyList(),
                mapOf("ES" to 20)
            )

            result shouldBe RequestLeaveError(OVERLAPPING_LEAVE).left()
        }
    }

    @Nested
    inner class AutoApprovePendingSickLeaves {

        @Test
        fun `should auto-approve pending sick leaves`() {
            val sickLeave = TestBuilders.buildLeavePeriod(
                days = listOf(MonthDay.of(1, 1)),
                status = PENDING,
                type = LeaveType.SICK
            )
            val annualLeave = TestBuilders.buildAnnualLeave(leaves = listOf(sickLeave))

            val result = annualLeave.autoApprovePendingSickLeaves()

            result shouldBe annualLeave.copy(
                leaves = listOf(sickLeave.copy(status = APPROVED))
            ).right()
        }

        @Test
        fun `should return error if no pending sick leaves found`() {
            val annualLeave = TestBuilders.buildAnnualLeave()

            val result = annualLeave.autoApprovePendingSickLeaves()

            result shouldBe AutoApproveError(NO_PENDING_LEAVES).left()
        }
    }
}