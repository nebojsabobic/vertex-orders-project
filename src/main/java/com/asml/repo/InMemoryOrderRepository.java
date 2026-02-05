package com.asml.repo;

import com.asml.domain.Order;
import com.asml.domain.Status;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();
    private final Vertx vertx;

    public InMemoryOrderRepository(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Future<Order> create(Order order) {
        Promise<Order> p = Promise.promise();
        vertx.runOnContext(v -> {
            store.put(order.id(), order);
            p.complete(order);
        });
        return p.future();
    }

    @Override
    public Future<Optional<Order>> findById(String id) {
        Promise<Optional<Order>> p = Promise.promise();
        vertx.runOnContext(v -> p.complete(Optional.ofNullable(store.get(id))));
        return p.future();
    }

    @Override
    public Future<List<Order>> find(Optional<String> customerId, Optional<Status> status, int limit) {
        Promise<List<Order>> p = Promise.promise();
        vertx.runOnContext(v -> {
            var list = store.values().stream()
                    .filter(o -> customerId.map(c -> c.equals(o.customerId())).orElse(true))
                    .filter(o -> status.map(s -> s == o.status()).orElse(true))
                    .sorted(Comparator.comparing(Order::createdAt).reversed())
                    .limit(limit)
                    .toList();
            p.complete(list);
        });
        return p.future();
    }

    @Override
    public Future<Order> update(Order order) {
        Promise<Order> p = Promise.promise();
        vertx.runOnContext(v -> {
            store.put(order.id(), order);
            p.complete(order);
        });
        return p.future();
    }
}
