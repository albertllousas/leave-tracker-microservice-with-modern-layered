package leavetracker.infrastructure.events

import leavetracker.business.domain.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class EventPublisher(private val applicationEventPublisher: ApplicationEventPublisher) {
    fun publish(event: DomainEvent) = applicationEventPublisher.publishEvent(event)
}