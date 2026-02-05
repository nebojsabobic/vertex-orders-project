package com.asml.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.math.BigDecimal;
import java.util.Map;

public class PriceService {

    private final Vertx vertx;
    private final Map<String, BigDecimal> prices = Map.of(
            "SKU1", new BigDecimal("10.50"),
            "SKU2", new BigDecimal("50.15"),
            "SKU3", new BigDecimal("12.25")
    );

    public PriceService(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<BigDecimal> getPrice(String sku) {
        Promise<BigDecimal> p = Promise.promise();
        vertx.setTimer(30, t -> {
            var price = prices.get(sku);
            if (price == null) p.fail("UNKNOWN_SKU");
            else p.complete(price);
        });
        return p.future();
    }
}
