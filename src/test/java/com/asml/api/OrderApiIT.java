package com.asml.api;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class OrderApiIT {

    private int port;
    private WebClient client;

    @BeforeEach
    void deploy(Vertx vertx, VertxTestContext tc) {
        port = 9000 + (int)(Math.random() * 1000);
        client = WebClient.create(vertx);

        DeploymentOptions opts = new DeploymentOptions()
                .setConfig(new JsonObject().put("http.port", port));

        vertx.deployVerticle(new MainVerticle(), opts)
                .onSuccess(id -> tc.completeNow())
                .onFailure(tc::failNow);
    }

    @Test
    void createThenGetById_returnsOrder(Vertx vertx, VertxTestContext tc) {
        JsonObject body = new JsonObject()
                .put("customerId", "c-123")
                .put("items", new JsonArray()
                        .add(new JsonObject().put("sku", "SKU1").put("qty", 2))
                        .add(new JsonObject().put("sku", "SKU1").put("qty", 1)));

        client.post(port, "localhost", "/orders")
                .putHeader("Content-Type", "application/json")
                .putHeader("X-Request-Id", "it-" + UUID.randomUUID())
                .sendJsonObject(body)
                .compose(res -> {
                    assertEquals(201, res.statusCode());
                    String orderId = res.bodyAsJsonObject().getString("id");
                    assertNotNull(orderId);

                    return client.get(port, "localhost", "/orders/" + orderId)
                            .putHeader("X-Request-Id", "it-" + UUID.randomUUID())
                            .send()
                            .map(getRes -> {
                                assertEquals(200, getRes.statusCode());
                                JsonObject order = getRes.bodyAsJsonObject();
                                assertEquals(orderId, order.getString("id"));
                                assertEquals("c-123", order.getString("customerId"));
                                assertEquals("CREATED", order.getString("status"));
                                return null;
                            });
                })
                .onSuccess(x -> tc.completeNow())
                .onFailure(tc::failNow);
    }

    @Test
    void listOrders_returnsArray(Vertx vertx, VertxTestContext tc) {
        JsonObject body = new JsonObject()
                .put("customerId", "c-999")
                .put("items", new JsonArray()
                        .add(new JsonObject().put("sku", "SKU1").put("qty", 1)));

        client.post(port, "localhost", "/orders")
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .compose(res -> {
                    assertEquals(201, res.statusCode());
                    return client.get(port, "localhost", "/orders?customerId=c-999&limit=20").send();
                })
                .onSuccess(listRes -> tc.verify(() -> {
                    assertEquals(200, listRes.statusCode());
                    JsonArray arr = listRes.bodyAsJsonArray();
                    assertTrue(arr.size() >= 1);
                    tc.completeNow();
                }))
                .onFailure(tc::failNow);
    }
}
