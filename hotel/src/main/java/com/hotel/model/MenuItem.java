package com.hotel.model;

import java.math.BigDecimal;

public record MenuItem(int id, String name, BigDecimal price) {
    @Override
    public String toString() {
        return name + " (Rs. " + price.stripTrailingZeros().toPlainString() + ")";
    }
}
