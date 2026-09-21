package com.eventhub.bookingservice.client;

import com.eventhub.bookingservice.dto.EventServiceResponseDTO;
import com.eventhub.bookingservice.exception.EventNotFoundException;
import com.eventhub.bookingservice.exception.EventServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@Component
public class EventClient {

    private final RestClient restClient;

    public EventClient(
            @Qualifier("loadBalancedRestClientBuilder")
            RestClient.Builder restClientBuilder) {

        this.restClient = restClientBuilder
                .baseUrl("http://event-service")
                .build();
    }

    @Retry(name = "eventService")
    @CircuitBreaker(
            name = "eventService",
            fallbackMethod = "eventServiceFallback"
    )
    public EventServiceResponseDTO getEventById(Long eventId) {

        // Helps learners observe each actual call/retry to Event Service.
        System.out.println(
                "Attempting Event Service call for eventId: " + eventId
        );

        try {
            return restClient.get()
                    .uri("/events/{id}", eventId)
                    .retrieve()
                    .body(EventServiceResponseDTO.class);

        } catch (HttpClientErrorException.NotFound ex) {

            // Event Service is available, but the requested event does not exist.
            throw new EventNotFoundException(
                    "Event not found with id: " + eventId
            );

        }
        catch (IllegalStateException | ResourceAccessException ex) {

            // Eureka/LoadBalancer could not find any running Event Service instance.
            throw new EventServiceUnavailableException(
                    "Event Service is currently unavailable"
            );

        } catch (RestClientException ex) {

            // Other HTTP failures such as Event Service returning 5xx.
            throw new EventServiceUnavailableException(
                    "Event Service is currently unavailable"
            );
        }
    }

    // Fallback when the Event Service call fails.
    private EventServiceResponseDTO eventServiceFallback(
            Long eventId,
            EventServiceUnavailableException ex) {

        System.out.println(
                "Fallback executed: Event Service is unavailable."
        );

        throw new EventServiceUnavailableException(
                "Unable to validate event " + eventId
                        + ". Please try again later."
        );
    }


    // Fallback when the Circuit Breaker is already OPEN.
    private EventServiceResponseDTO eventServiceFallback(
            Long eventId,
            CallNotPermittedException ex) {

        System.out.println(
                "Fallback executed: Circuit Breaker is OPEN."
        );

        throw new EventServiceUnavailableException(
                "Event Service is temporarily unavailable. "
                        + "Please try again later."
        );
    }
}