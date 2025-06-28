package leavetracker.infrastructure.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import leavetracker.business.domain.LeavePeriod
import leavetracker.business.domain.LeavePeriodStatus
import leavetracker.business.domain.LeaveType
import leavetracker.fixtures.Postgres
import leavetracker.fixtures.TestBuilders
import leavetracker.infrastructure.defaultObjectMapper
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.time.MonthDay
import java.time.Year
import java.util.UUID

@Tag("integration")
@TestInstance(PER_CLASS)
class AnnualLeaveRepositoryTest {

    private val postgres = Postgres()

    private val repository = AnnualLeaveRepository(postgres.jdbcTemplate, defaultObjectMapper)

    @Test
    fun `should save and find an annual leave`() {
        val annualLeave = TestBuilders.buildAnnualLeave()

        repository.save(annualLeave)
        val result = repository.findBy(annualLeave.employeeId, annualLeave.year)

        result shouldBe annualLeave.copy(version = 1)
    }

    @Test
    fun `should not find an annual leave when it does not exists`() {
        repository.findBy(UUID.randomUUID(), Year.of(2042)) shouldBe null
    }

    @Test
    fun `should update a customer`() {
        val annualLeave = TestBuilders.buildAnnualLeave()
            .also(repository::save)
            .let { repository.findBy(it.employeeId, it.year)!! }
        val updated = annualLeave.copy(
            leaves = listOf(
                LeavePeriod(
                    days = listOf(MonthDay.of(12, 1)),
                    type = LeaveType.SICK,
                    status = LeavePeriodStatus.APPROVED)
            ),
        )

        repository.save(updated)
        val result = repository.findBy(annualLeave.employeeId, annualLeave.year)

        result shouldBe updated.copy(version = 2)
    }

    @Test
    fun `should throw OptimisticLockingException when updating a customer with wrong version`() {
        val annualLeave = TestBuilders.buildAnnualLeave()
            .also(repository::save)
            .let { repository.findBy(it.employeeId, it.year)!! }
        val updated = annualLeave.copy(version = 3)

        shouldThrow<OptimisticLockingException> { repository.save(updated) }
    }
}
