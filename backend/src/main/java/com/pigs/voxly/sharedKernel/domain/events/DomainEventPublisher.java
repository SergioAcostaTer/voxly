package com.pigs.voxly.sharedKernel.domain.events;

import java.util.Collection;

public interface DomainEventPublisher {

    void publish(DomainEvent event);

    void publishAll(Collection<DomainEvent> events);
}
