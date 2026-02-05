package com.asml.service;

import com.asml.domain.Order;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.Random;

public class InventoryService {
    private final Vertx vertx;
    private final Random rnd = new Random();
    private final int failureRatePercent;

    public InventoryService(Vertx vertx) {
        this(vertx, 10);
    }

    public InventoryService(Vertx vertx, int failureRatePercent) {
        this.vertx = vertx;
        this.failureRatePercent = Math.max(0, Math.min(100, failureRatePercent));
    }

    public Future<Void> reserve(Order order) {
        Promise<Void> p = Promise.promise();

        vertx.setTimer(80, t -> {
            if (shouldFail()) {
                p.fail("INVENTORY_RESERVATION_FAILED");
            } else {
                p.complete();
            }
        });

        return p.future();
    }

    private boolean shouldFail() {
        if (failureRatePercent == 0) return false;
        return rnd.nextInt(100) < failureRatePercent;
    }
}

