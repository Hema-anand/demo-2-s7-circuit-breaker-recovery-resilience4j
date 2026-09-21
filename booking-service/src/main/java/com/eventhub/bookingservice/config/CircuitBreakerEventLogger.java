package com.eventhub.bookingservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerEventLogger {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerEventLogger(
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostConstruct
    public void registerCircuitBreakerEvents() {

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("eventService");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        System.out.println(
                                "Circuit Breaker State Changed: "
                                        + event.getStateTransition()
                        )
                );
    }
}