package leavetracker.infrastructure.observability

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.spyk
import io.mockk.verify
import leavetracker.business.domain.LeaveAutoApproved
import leavetracker.business.domain.LeaveRequested
import leavetracker.business.domain.LeaveType
import leavetracker.fixtures.TestBuilders
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean

@Tag("integration")
@SpringBootTest(classes = [Monitor::class, TestConfig::class])
class MonitorTest {

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var logger: Logger

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Test
    fun `should monitor when a leave is requested successfully`() {
        val annualLeave = TestBuilders.buildAnnualLeave()
        val event = LeaveRequested(annualLeave, emptyList(), LeaveType.VACATION)

        eventPublisher.publishEvent(event)

        verify { logger.info("domain-event: 'LeaveRequested', id:'${annualLeave.id}', type:'VACATION'") }
        meterRegistry.get("leavetracker.events")
            .tags("type", "VACATION")
            .tags("event", "LeaveRequested")
            .counter()
            .count() shouldBe 1.0
    }

    @Test
    fun `should monitor when a leave is auto-approved successfully`() {
        val annualLeave = TestBuilders.buildAnnualLeave()
        val event = LeaveAutoApproved(annualLeave)

        eventPublisher.publishEvent(event)

        verify { logger.info("domain-event: 'LeaveAutoApproved', id:'${annualLeave.id}'") }
        meterRegistry.get("leavetracker.events")
            .tags("event", "LeaveAutoApproved")
            .counter()
            .count() shouldBe 1.0
    }
}

class TestConfig {
    @Bean
    fun logger(): Logger = spyk(NOPLogger.NOP_LOGGER)

    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
}
