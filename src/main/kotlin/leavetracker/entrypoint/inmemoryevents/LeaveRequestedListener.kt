package leavetracker.entrypoint.inmemoryevents

import leavetracker.business.domain.LeaveRequested
import leavetracker.business.services.AutoApproveLeaveCommand
import leavetracker.business.services.AutoApproveLeaveService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@Component
class LeaveRequestedListener(
    private val autoApproveLeave: AutoApproveLeaveService
) {

    @EventListener
    fun reactTo(event: LeaveRequested) {
        //TODO: monitor errors
        autoApproveLeave(AutoApproveLeaveCommand(event.annualLeave.employeeId, event.annualLeave.year))
    }
}