package leavetracker.entrypoint.inmemoryevents

import io.mockk.mockk
import io.mockk.verify
import leavetracker.business.domain.LeaveAutoApproved
import leavetracker.business.domain.LeaveRequested
import leavetracker.business.domain.LeaveType
import leavetracker.business.services.AutoApproveLeaveCommand
import leavetracker.business.services.AutoApproveLeaveService
import leavetracker.fixtures.TestBuilders
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean


@Tag("integration")
@SpringBootTest(classes = [LeaveRequestedListener::class, TestConfig::class])
class LeaveRequestedListenerTest {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var autoApproveLeaveService: AutoApproveLeaveService

    @Test
    fun `should trigger auto-approve when a request has been requested`() {
        val annualLeave = TestBuilders.buildAnnualLeave()
        val event = LeaveRequested(annualLeave, emptyList(), LeaveType.VACATION)

        eventPublisher.publishEvent(event)

        verify {
            autoApproveLeaveService.invoke(
                AutoApproveLeaveCommand(event.annualLeave.employeeId, event.annualLeave.year)
            )
        }
    }
}

class TestConfig {
    @Bean
    fun autoApproveLeaveService(): AutoApproveLeaveService = mockk(relaxed = true)
}
