package com.hotel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A calculated bill for one stay. Produced by HotelService; formatted by BillFormatter. */
public record Bill(
        Room room,
        Booking booking,
        long nights,
        BigDecimal roomCharge,
        List<FoodLine> foodLines,
        BigDecimal foodTotal,
        BigDecimal grandTotal,
        LocalDateTime checkOut) {
}
