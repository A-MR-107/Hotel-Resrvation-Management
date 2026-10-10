package com.hotel.model;

import java.time.LocalDateTime;

public record Booking(long id, int roomNumber, Guest guest1, Guest guest2, LocalDateTime checkIn) {
}
