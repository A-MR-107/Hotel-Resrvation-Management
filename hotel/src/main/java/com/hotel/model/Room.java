package com.hotel.model;

public record Room(int number, RoomType type, Booking activeBooking) {
    public boolean occupied() {
        return activeBooking != null;
    }

    public String label() {
        String base = "Room " + number + " - " + type.name();
        return occupied() ? base + " - " + activeBooking.guest1().name() : base;
    }

    @Override
    public String toString() {
        return label();
    }
}
