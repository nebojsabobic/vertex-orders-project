package com.asml.api;

import com.asml.domain.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.asml.api.dto.CreateOrderRequest;
import com.asml.error.ApiException;
import com.asml.service.OrderService;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OrderRoutes {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final OrderService orderService;

    public OrderRoutes(OrderService orderService) {
        this.orderService = orderService;
    }

    public Router router(Vertx vertx) {
        Router r = Router.router(vertx);


        // Request ID
        r.route().handler(ctx -> {
            String rid = ctx.request().getHeader("X-Request-Id");
            if (rid == null || rid.isBlank()) rid = UUID.randomUUID().toString();
            ctx.put("rid", rid);
            ctx.response().putHeader("X-Request-Id", rid);
            ctx.next();
        });

        r.post("/orders").handler(this::createOrder);
        r.get("/orders/:id").handler(this::getOrderById);
        r.get("/orders").handler(this::listOrders);
        r.post("/orders/:id/confirm").handler(this::confirmOrder);

        r.post("/orders/:id/process").handler(this::processOrder);

        r.route().failureHandler(this::handleFailure);

        return r;
    }

    private void createOrder(RoutingContext ctx) {
        try {
            CreateOrderRequest req = mapper.readValue(ctx.body().asString(), CreateOrderRequest.class);

            orderService.create(req)
                    .onSuccess(order -> {
                        ctx.response()
                                .setStatusCode(201)
                                .putHeader("Content-Type", "application/json")
                                .end(toJson(Map.of(
                                        "id", order.id(),
                                        "status", order.status().name(),
                                        "total", order.total()
                                )));
                    })
                    .onFailure(ctx::fail);

        } catch (Exception e) {
            ctx.fail(new ApiException(400, "INVALID_JSON"));
        }
    }

    private void getOrderById(RoutingContext ctx) {
        String id = ctx.pathParam("id");

        orderService.getById(id)
                .onSuccess(order -> {
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(toJson(order));
                })
                .onFailure(ctx::fail);
    }

    private void listOrders(RoutingContext ctx) {
        String customerId = ctx.queryParams().get("customerId");
        String statusStr = ctx.queryParams().get("status");
        String limitStr = ctx.queryParams().get("limit");

        int limit = 20;
        if (limitStr != null && !limitStr.isBlank()) {
            try { limit = Integer.parseInt(limitStr); }
            catch (Exception e) { ctx.fail(new ApiException(400, "LIMIT_INVALID")); return; }
        }

        var customerOpt = (customerId == null || customerId.isBlank())
                ? Optional.<String>empty()
                : Optional.of(customerId);

        var statusOpt = Optional.<Status>empty();
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                statusOpt = Optional.of(Status.valueOf(statusStr.trim().toUpperCase()));
            } catch (Exception e) {
                ctx.fail(new ApiException(400, "STATUS_INVALID"));
                return;
            }
        }

        orderService.list(customerOpt, statusOpt, limit)
                .onSuccess(list -> {
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(toJson(list));
                })
                .onFailure(ctx::fail);
    }

    private void confirmOrder(RoutingContext ctx) {
        String id = ctx.pathParam("id");

        orderService.confirm(id)
                .onSuccess(order -> {
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(toJson(order));
                })
                .onFailure(ctx::fail);
    }

    private void processOrder(RoutingContext ctx) {
        String id = ctx.pathParam("id");

        orderService.process(id)
                .onSuccess(order -> {
                    ctx.response()
                            .setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(toJson(order));
                })
                .onFailure(ctx::fail);
    }

    private void handleFailure(RoutingContext ctx) {
        Throwable t = ctx.failure();

        int status = 500;
        String code = "INTERNAL_ERROR";

        if (t instanceof ApiException ae) {
            status = ae.status;
            code = ae.code;
        } else if (t != null && "UNKNOWN_SKU".equals(t.getMessage())) {
            status = 400;
            code = "UNKNOWN_SKU";
        }

        String rid = ctx.get("rid");     // <- explicit type
        if (rid == null) rid = "n/a";

        ctx.response()
                .setStatusCode(status)
                .putHeader("Content-Type", "application/json")
                .end(toJson(Map.of(
                        "error", code,
                        "requestId", rid
                )));
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            e.printStackTrace(); // temporary
            return "{\"error\":\"SERIALIZATION_ERROR\"}";
        }
    }
}
