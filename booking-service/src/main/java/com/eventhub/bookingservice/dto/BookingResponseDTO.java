package com.eventhub.bookingservice.dto;

import com.eventhub.bookingservice.entity.BookingStatus;
import java.time.LocalDate;

public class BookingResponseDTO {

    private Long id;
    private Long eventId;
    private Long customerId;
    private Integer numberOfTickets;
    private LocalDate bookingDate;
    private BookingStatus status;

    public BookingResponseDTO() {
    }

    public BookingResponseDTO(Long id, Long eventId, Long customerId, Integer numberOfTickets,
                             LocalDate bookingDate, BookingStatus status) {
        this.id = id;
        this.eventId = eventId;
        this.customerId = customerId;
        this.numberOfTickets = numberOfTickets;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
        return numberOfTickets;
    }

    public void setNumberOfTickets(Integer numberOfTickets) {
        this.numberOfTickets = numberOfTickets;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
