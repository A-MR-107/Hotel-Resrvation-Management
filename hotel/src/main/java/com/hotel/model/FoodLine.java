package com.hotel.model;

import java.math.BigDecimal;

public record FoodLine(String itemName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}
