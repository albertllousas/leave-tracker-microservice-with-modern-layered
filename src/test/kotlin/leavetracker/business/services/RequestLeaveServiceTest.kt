package leavetracker.business.services

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import leavetracker.business.domain.AnnualLeave
import leavetracker.business.domain.LeavePeriod
import leavetracker.business.domain.LeavePeriodStatus.PENDING
import leavetracker.business.domain.LeaveRequested
import leavetracker.business.domain.LeaveType.VACATION
import leavetracker.fixtures.TestBuilders.buildAnnualLeave
import leavetracker.fixtures.TestBuilders.buildEmployee
import leavetracker.fixtures.TestBuilders.buildRequestLeaveCommand
import leavetracker.infrastructure.clients.external.PublicHolidaysClient
import leavetracker.infrastructure.clients.internal.EmployeeClient
import leavetracker.infrastructure.db.AnnualLeaveRepository
import leavetracker.infrastructure.events.EventPublisher
import org.junit.jupiter.api.Test
import java.time.MonthDay
import java.util.UUID

class RequestLeaveServiceTest {

    private val repository: AnnualLeaveRepository = mockk(relaxUnitFun = true)

    private val eventPublisher: EventPublisher = mockk(relaxUnitFun = true)

    private val employeeClient: EmployeeClient = mockk()

    private val publicHolidaysClient: PublicHolidaysClient = mockk()

    private val fixedId = UUID.randomUUID()

    private val requestLeave = RequestLeaveService(
        repository = repository,
        employeeClient = employeeClient,
        publicHolidaysClient = publicHolidaysClient,
        genId = { fixedId },
        eventPublisher = eventPublisher,
        leaveDaysPerCountry = mapOf("ES" to 24)
    )

    @Test
    fun `should successfully orchestrate a leave request for a non existent annual leave`() {
        val cmd = buildRequestLeaveCommand()
        val employee = buildEmployee()
        every { repository.findBy(cmd.employeeId, cmd.year) } returns null
        every { employeeClient.get(cmd.employeeId) } returns employee
        every { publicHolidaysClient.get(cmd.year, employee.countryCode) } returns listOf(MonthDay.of(10, 1))

        val result = requestLeave(cmd)

        result shouldBe Unit.right()
        verify {
            repository.save(
                AnnualLeave(
                    id = fixedId,
                    employeeId = cmd.employeeId,
                    year = cmd.year,
                    leaves = listOf(LeavePeriod(listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)), VACATION, PENDING, null))
                )
            )
            eventPublisher.publish(any<LeaveRequested>())
        }
    }

    @Test
    fun `should successfully orchestrate a leave request for an existent annual leave`() {
        val cmd = buildRequestLeaveCommand()
        val employee = buildEmployee()
        val annualLeave = buildAnnualLeave(leaves = emptyList())
        every { repository.findBy(cmd.employeeId, cmd.year) } returns annualLeave
        every { employeeClient.get(cmd.employeeId) } returns employee
        every { publicHolidaysClient.get(cmd.year, employee.countryCode) } returns listOf(MonthDay.of(10, 1))

        val result = requestLeave(cmd)

        result shouldBe Unit.right()
        verify {
            repository.save(
                annualLeave.copy(
                    leaves = listOf(LeavePeriod(listOf(MonthDay.of(1, 1), MonthDay.of(1, 2)), VACATION, PENDING, null))
                )
            )
            eventPublisher.publish(any<LeaveRequested>())
        }
    }

    @Test
    fun `should fail orchestrating a leave request when actual requesting fails`() {
        val cmd = buildRequestLeaveCommand(days = emptyList())
        val employee = buildEmployee()
        every { repository.findBy(cmd.employeeId, cmd.year) } returns null
        every { employeeClient.get(cmd.employeeId) } returns employee
        every { publicHolidaysClient.get(cmd.year, employee.countryCode) } returns listOf(MonthDay.of(10, 1))

        val result = requestLeave(cmd)

        result.isLeft() shouldBe true
    }
}
