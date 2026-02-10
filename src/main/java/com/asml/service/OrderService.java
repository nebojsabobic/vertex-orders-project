package com.asml.service;

import com.asml.api.dto.CreateOrderRequest;
import com.asml.domain.Order;
import com.asml.domain.OrderItem;
import com.asml.domain.Status;
import com.asml.error.ApiException;
import com.asml.repo.OrderRepository;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class OrderService {
    private final OrderRepository repo;
    private final PriceService priceService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final Vertx vertx;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    public OrderService(Vertx vertx,
                        OrderRepository repo,
                        PriceService priceService,
                        InventoryService inventoryService,
                        PaymentService paymentService) {
        this.vertx = vertx;
        this.repo = repo;
        this.priceService = priceService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
    }

    public Future<Order> create(CreateOrderRequest req) {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());
        validateCreate(req);

        List<OrderItem> items = req.items().stream()
                .map(i -> new OrderItem(i.sku(), i.qty()))
                .toList();

        List<Future<BigDecimal>> totals = items.stream()
                .map(i -> priceService.getPrice(i.sku())
                        .map(p -> p.multiply(BigDecimal.valueOf(i.qty()))))
                .toList();

        return Future.all(totals)
                .map(cf -> {
                    BigDecimal total = cf.list().stream()
                            .map(v -> (BigDecimal) v)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Instant now = Instant.now();
                    Order order = new Order(
                            UUID.randomUUID().toString(),
                            req.customerId(),
                            items,
                            Status.CREATED,
                            total,
                            now,
                            now
                    );

                    return order;
                })
                .compose(repo::create)
                .onSuccess(created -> {
                    vertx.eventBus().publish("order.created",
                            new io.vertx.core.json.JsonObject()
                                    .put("orderId", created.id())
                                    .put("customerId", created.customerId())
                                    .put("total", created.total().toString())
                    );
                });
    }

    public Future<Order> getById(String id) {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());

        if (id == null || id.isBlank()) return Future.failedFuture(new ApiException(400, "ID_REQUIRED"));
        return repo.findById(id).compose(opt -> {
            if (opt.isEmpty()) return Future.failedFuture(new ApiException(404, "ORDER_NOT_FOUND"));
            return Future.succeededFuture(opt.get());
        });
    }

    public Future<List<Order>> list(Optional<String> customerId, Optional<Status> status, int limit) {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return repo.find(customerId, status, safeLimit);
    }

    public Future<Order> confirm(String id) {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());

        return getById(id).compose(o -> {
            if (o.status() == Status.CONFIRMED) return Future.failedFuture(new ApiException(409, "ALREADY_CONFIRMED"));
            if (o.status() != Status.CREATED) return Future.failedFuture(new ApiException(409, "INVALID_STATE"));
            Order updated = new Order(
                    o.id(), o.customerId(), o.items(),
                    Status.CONFIRMED,
                    o.total(),
                    o.createdAt(),
                    Instant.now()
            );
            return repo.update(updated);
        });
    }

    public Future<Order> process(String id) {
        log.info("[THREAD NAME]: {}", Thread.currentThread().getName());

        if (inventoryService == null || paymentService == null || vertx == null) {
            return Future.failedFuture(new ApiException(501, "PROCESS_NOT_ENABLED"));
        }

        return getById(id).compose(o -> {
            if (o.status() != Status.CONFIRMED) return Future.failedFuture(new ApiException(409, "ORDER_NOT_CONFIRMED"));

            Future<Void> inv = inventoryService.reserve(o);
            Future<Void> pay = paymentService.authorize(o);

            return CompositeFuture.all(inv, pay)
                    .compose(x -> {
                        Order processed = new Order(
                                o.id(), o.customerId(), o.items(),
                                Status.PROCESSED,
                                o.total(),
                                o.createdAt(),
                                Instant.now()
                        );
                        return repo.update(processed);
                    })
                    .onSuccess(updated -> vertx.eventBus().publish("order.processed", new JsonObject()
                            .put("orderId", updated.id())
                            .put("customerId", updated.customerId())
                            .put("total", updated.total().toString())));
        });
    }

    private static void validateCreate(CreateOrderRequest req) {

        if (req == null) throw new ApiException(400, "INVALID_BODY");
        if (req.customerId() == null || req.customerId().isBlank())
            throw new ApiException(400, "CUSTOMER_ID_REQUIRED");
        if (req.items() == null || req.items().isEmpty())
            throw new ApiException(400, "ITEMS_REQUIRED");

        for (var item : req.items()) {
            if (item == null) throw new ApiException(400, "INVALID_ITEM");
            if (item.sku() == null || item.sku().isBlank()) throw new ApiException(400, "SKU_REQUIRED");
            if (item.qty() <= 0) throw new ApiException(400, "QTY_INVALID");
        }
    }
}
