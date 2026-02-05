package com.asml.api.dto;

import java.util.List;

public record CreateOrderRequest(String customerId, List<Item> items) {
    public record Item(String sku, int qty) {}
}
