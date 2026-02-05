package com.asml.api;

import com.asml.repo.InMemoryOrderRepository;
import com.asml.service.InventoryService;
import com.asml.service.OrderService;
import com.asml.service.PaymentService;
import com.asml.service.PriceService;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {
        int port = config().getInteger("http.port", 8080);

        var repo = new InMemoryOrderRepository(vertx);
        var price = new PriceService(vertx);

        var inv = new InventoryService(vertx);
        var pay = new PaymentService(vertx);

        var service = new OrderService(vertx, repo, price, inv, pay);

        var orderRoutes = new OrderRoutes(service);

        Router root = Router.router(vertx);
        root.route().handler(BodyHandler.create());

        root.get("/health").handler(ctx -> ctx.response().end("OK"));
        root.mountSubRouter("/", orderRoutes.router(vertx));

        vertx.createHttpServer()
                .requestHandler(root)
                .listen(port)
                .onSuccess(s -> System.out.println("Server started on 8080"))
                .onFailure(Throwable::printStackTrace);
    }
}