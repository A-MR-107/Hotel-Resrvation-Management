package com.hotel.service;

/** A business-rule violation that is safe to show to the user as-is. */
public class HotelException extends RuntimeException {
    public HotelException(String message) {
        super(message);
    }
}
