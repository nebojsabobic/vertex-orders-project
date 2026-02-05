package com.asml.service;

import com.asml.api.dto.CreateOrderRequest;
import com.asml.domain.Order;
import com.asml.domain.Status;
import com.asml.error.ApiException;
import com.asml.repo.OrderRepository;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
class OrderServiceTest {

    @Test
    void createOrder_calculatesTotal_andStoresOrder(Vertx vertx, VertxTestContext tc) {
        OrderRepository repo = mock(OrderRepository.class);

        PriceService priceService = new PriceService(vertx);

        OrderService service = new OrderService(vertx, repo, priceService, null, null);

        when(repo.create(any(Order.class)))
                .thenAnswer(inv -> Future.succeededFuture(inv.getArgument(0)));

        var req = new CreateOrderRequest("c-123", List.of(
                new CreateOrderRequest.Item("SKU1", 2), // 10.50 * 2 = 21.00
                new CreateOrderRequest.Item("SKU2", 1)  // 50.15 * 1 = 50.15
        ));

        service.create(req)
                .onSuccess(order -> tc.verify(() -> {
                    assertNotNull(order.id());
                    assertEquals("c-123", order.customerId());
                    assertEquals(Status.CREATED, order.status());
                    assertEquals(new BigDecimal("71.15"), order.total()); // 21.00 + 50.15

                    verify(repo, times(1)).create(any(Order.class));
                    tc.completeNow();
                }))
                .onFailure(tc::failNow);
    }

    @Test
    void confirmOrder_whenNotFound_returns404(Vertx vertx, VertxTestContext tc) {
        OrderRepository repo = mock(OrderRepository.class);
        PriceService priceService = new PriceService(vertx);

        OrderService service = new OrderService(vertx, repo, priceService, null, null);

        when(repo.findById("missing")).thenReturn(Future.succeededFuture(Optional.empty()));

        service.confirm("missing")
                .onSuccess(o -> tc.failNow(new AssertionError("Expected failure")))
                .onFailure(err -> tc.verify(() -> {
                    assertTrue(err instanceof ApiException);
                    ApiException ae = (ApiException) err;
                    assertEquals(404, ae.status);
                    assertEquals("ORDER_NOT_FOUND", ae.code);
                    tc.completeNow();
                }));
    }
}