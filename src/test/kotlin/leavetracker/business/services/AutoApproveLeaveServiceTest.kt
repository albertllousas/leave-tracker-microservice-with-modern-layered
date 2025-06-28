package leavetracker.business.services

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import leavetracker.business.domain.AutoApproveError
import leavetracker.business.domain.AutoApproveError.AutoApproveErrorType.ANNUAL_LEAVE_NOT_FOUND
import leavetracker.business.domain.AutoApproveError.AutoApproveErrorType.NO_PENDING_LEAVES
import leavetracker.business.domain.LeavePeriodStatus.APPROVED
import leavetracker.business.domain.LeavePeriodStatus.PENDING
import leavetracker.business.domain.LeaveRequested
import leavetracker.business.domain.LeaveType.SICK
import leavetracker.fixtures.TestBuilders.buildAnnualLeave
import leavetracker.fixtures.TestBuilders.buildLeavePeriod
import leavetracker.infrastructure.db.AnnualLeaveRepository
import leavetracker.infrastructure.events.EventPublisher
import org.junit.jupiter.api.Test
import java.time.Year
import java.util.UUID

class AutoApproveLeaveServiceTest {

    private val repository: AnnualLeaveRepository = mockk(relaxUnitFun = true)

    private val eventPublisher: EventPublisher = mockk(relaxUnitFun = true)

    private val autoApproveLeave = AutoApproveLeaveService(
        repository = repository,
        eventPublisher = eventPublisher,
    )

    @Test
    fun `should successfully orchestrate an auto-approve for a an annual leave`() {
        val cmd = AutoApproveLeaveCommand(UUID.randomUUID(), Year.of(2023))
        val leavePeriod = buildLeavePeriod(status = PENDING, type = SICK)
        val annualLeave = buildAnnualLeave(leaves = listOf(leavePeriod))
        every { repository.findBy(cmd.employeeId, cmd.year) } returns annualLeave

        val result = autoApproveLeave(cmd)

        result shouldBe Unit.right()
        verify {
            repository.save(
                annualLeave.copy(
                    leaves = listOf(leavePeriod.copy(status = APPROVED))
                )
            )
            eventPublisher.publish(any<LeaveRequested>())
        }
    }


    @Test
    fun `should fail orchestrating an auto-approve when actual auto-approving fails`() {
        val cmd = AutoApproveLeaveCommand(UUID.randomUUID(), Year.of(2023))
        val leavePeriod = buildLeavePeriod(status = APPROVED, type = SICK)
        val annualLeave = buildAnnualLeave(leaves = listOf(leavePeriod))
        every { repository.findBy(cmd.employeeId, cmd.year) } returns annualLeave

        val result = autoApproveLeave(cmd)

        result.isLeft() shouldBe true
    }

    @Test
    fun `should fail orchestrating an auto-approve when annual leave is not found`() {
        val cmd = AutoApproveLeaveCommand(UUID.randomUUID(), Year.of(2023))
        every { repository.findBy(cmd.employeeId, cmd.year) } returns null

        val result = autoApproveLeave(cmd)

        result shouldBe AutoApproveError(ANNUAL_LEAVE_NOT_FOUND).left()
    }

}