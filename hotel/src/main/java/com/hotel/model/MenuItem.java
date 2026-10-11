package com.hotel.model;

import java.math.BigDecimal;

public record MenuItem(int id, String name, BigDecimal price, String category) {

    public boolean drink() {
        return "DRINK".equals(category);
    }

    @Override
    public String toString() {
        return name;
    }
}
