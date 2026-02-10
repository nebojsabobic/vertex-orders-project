package com.asml.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

public class PriceService {

    private final Vertx vertx;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final Map<String, BigDecimal> prices = Map.of(
            "SKU1", new BigDecimal("10.50"),
            "SKU2", new BigDecimal("50.15"),
            "SKU3", new BigDecimal("12.25")
    );

    public PriceService(Vertx vertx) {
        this.vertx = vertx;
    }

    public Future<BigDecimal> getPrice(String sku) {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());

        Promise<BigDecimal> p = Promise.promise();
        vertx.setTimer(500, t -> {
            var price = prices.get(sku);
            if (price == null) p.fail("UNKNOWN_SKU");
            else p.complete(price);
        });
        return p.future();
    }
}
