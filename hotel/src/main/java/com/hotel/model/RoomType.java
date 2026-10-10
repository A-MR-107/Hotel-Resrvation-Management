package com.hotel.model;

import java.math.BigDecimal;

public record RoomType(String code, String name, BigDecimal pricePerDay, int capacity, String description) {
    public boolean twoGuests() {
        return capacity >= 2;
    }
}
