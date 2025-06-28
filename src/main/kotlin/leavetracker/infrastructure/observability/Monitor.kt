package leavetracker.infrastructure.observability

import io.micrometer.core.instrument.MeterRegistry
import leavetracker.business.domain.DomainEvent
import leavetracker.business.domain.LeaveAutoApproved
import leavetracker.business.domain.LeaveRequested
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.lang.invoke.MethodHandles

@Component
class Monitor(
    private val meterRegistry: MeterRegistry,
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
) {

    @EventListener
    fun reactTo(domainEvent: DomainEvent) {
        when (domainEvent) {
            is LeaveRequested -> {
                logger.info("domain-event: 'LeaveRequested', id:'${domainEvent.annualLeave.id}', type:'${domainEvent.type}'")
                meterRegistry.counter("leavetracker.events", "event", "LeaveRequested", "type", domainEvent.type.name).increment()
            }

            is LeaveAutoApproved -> {
                logger.info("domain-event: 'LeaveAutoApproved', id:'${domainEvent.annualLeave.id}'")
                meterRegistry.counter("leavetracker.events", "event", "LeaveAutoApproved").increment()
            }
        }
    }
}