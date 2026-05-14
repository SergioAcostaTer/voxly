package com.pigs.voxly.sharedKernel.infrastructure;

import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;
import com.pigs.voxly.sharedKernel.domain.events.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(Collection<DomainEvent> events) {
        events.forEach(applicationEventPublisher::publishEvent);
    }
}
