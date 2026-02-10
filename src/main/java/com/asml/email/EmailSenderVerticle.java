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
            sendEmail("Order Created", (JsonObject) msg.body());
        });

        vertx.eventBus().consumer("order.processed", msg -> {
            sendEmail("Order Processed", (JsonObject) msg.body());
        });
    }

    private void sendEmail(String type, JsonObject payload) {

        String jobId = UUID.randomUUID().toString();
        String orderId = payload.getString("orderId");

        log.info("[email:{}] {} event received for order {}", jobId, type, orderId);

        // simulate async email sending
        vertx.setTimer(300, t -> {
            log.info("[email:{}] {} email sent successfully for order {}", jobId, type, orderId);
        });
    }
}
