package com.asml.email;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class EmailSenderVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderVerticle.class);

    @Override
    public void start() {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());

        vertx.eventBus().consumer("order.created", msg -> {
            JsonObject payload = (JsonObject) msg.body();

            String emailJobId = UUID.randomUUID().toString();
            String orderId = payload.getString("orderId");
            String customerId = payload.getString("customerId");
            String total = payload.getString("total");

            log.info("[email:{}] Received order.created for orderId={}, customerId={}, total={}",
                    emailJobId, orderId, customerId, total);

            // Simulate email sending (async delay)
            vertx.setTimer(200, t -> {
                log.info("[email:{}] Email sent successfully for orderId={}", emailJobId, orderId);
            });
        });
    }
}
