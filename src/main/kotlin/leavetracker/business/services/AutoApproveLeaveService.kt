package leavetracker.business.services

import arrow.core.Either
import arrow.core.left
import leavetracker.business.domain.AutoApproveError
import leavetracker.business.domain.AutoApproveError.AutoApproveErrorType.ANNUAL_LEAVE_NOT_FOUND
import leavetracker.business.domain.LeaveAutoApproved
import leavetracker.infrastructure.db.AnnualLeaveRepository
import leavetracker.infrastructure.events.EventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Year
import java.util.UUID

@Service
class AutoApproveLeaveService(
    private val repository: AnnualLeaveRepository,
    private val eventPublisher: EventPublisher
) {

    @Transactional
    operator fun invoke(cmd: AutoApproveLeaveCommand): Either<AutoApproveError, Unit> =
        repository.findBy(cmd.employeeId, cmd.year)
            ?.autoApprovePendingSickLeaves()
            ?.onRight { repository.save(it) }
            ?.onRight { eventPublisher.publish(LeaveAutoApproved(it)) }
            ?.map { Unit }
            ?: AutoApproveError(ANNUAL_LEAVE_NOT_FOUND).left()
}

data class AutoApproveLeaveCommand(val employeeId: UUID, val year: Year)