package com.eventhub.bookingservice.dto;

import com.eventhub.bookingservice.entity.BookingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BookingRequestDTO {

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Integer getNumberOfTickets() {
        return quantity;
    }

    public void setNumberOfTickets(Integer numberOfTickets) {
        this.quantity = numberOfTickets;
    }
    public record BookingStatusUpdateDTO(
            @NotNull BookingStatus status
    ) {
    }
}
