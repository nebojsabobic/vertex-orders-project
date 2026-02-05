package com.asml.repo;

import com.asml.domain.Order;
import com.asml.domain.Status;
import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Future<Order> create(Order order);
    Future<Optional<Order>> findById(String id);
    Future<List<Order>> find(Optional<String> customerId, Optional<Status> status, int limit);
    Future<Order> update(Order order);
}