package com.asml.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        String id,
        String customerId,
        List<OrderItem> items,
        Status status,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) { }
