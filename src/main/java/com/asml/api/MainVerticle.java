package com.asml.api;

import com.asml.repo.InMemoryOrderRepository;
import com.asml.service.InventoryService;
import com.asml.service.OrderService;
import com.asml.service.PaymentService;
import com.asml.service.PriceService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(io.vertx.core.Promise<Void> startPromise) {

        int port = config()
                .getJsonObject("http", new JsonObject())
                .getInteger("port", 8080);

        String expectedApiKey = config()
                .getJsonObject("api", new JsonObject())
                .getString("key", "super-secret-key");

        boolean rlEnabled = config()
                .getJsonObject("rateLimit", new JsonObject())
                .getBoolean("enabled", true);

        int rpm = config()
                .getJsonObject("rateLimit", new JsonObject())
                .getInteger("requestsPerMinute", 60);

        var rateLimiter = new com.asml.middleware.RateLimitHandler(rlEnabled, rpm);
        vertx.setPeriodic(60_000, id -> rateLimiter.reset());

        var repo = new InMemoryOrderRepository(vertx);
        var price = new PriceService(vertx);

        var inv = new InventoryService(vertx);
        var pay = new PaymentService(vertx);

        var service = new OrderService(vertx, repo, price, inv, pay);

        var orderRoutes = new OrderRoutes(service);

        Router root = Router.router(vertx);

        root.get("/health").handler(ctx -> ctx.response().end("OK"));

        root.route().handler(ctx -> {
            // Skip health
            if ("/health".equals(ctx.normalisedPath())) {
                ctx.next();
                return;
            }

            String apiKey = ctx.request().getHeader("X-Api-Key");

            if (!expectedApiKey.equals(apiKey)) {
                ctx.response()
                        .setStatusCode(401)
                        .putHeader("Content-Type", "application/json")
                        .end("{\"error\":\"UNAUTHORIZED\"}");
                return;
            }

            ctx.next();
        });

        // rate limiter (protects everything except /health)
        root.route().handler(ctx -> {
            if ("/health".equals(ctx.normalisedPath())) {
                ctx.next();
                return;
            }
            rateLimiter.handle(ctx);
        });

        root.route().handler(BodyHandler.create());
        root.mountSubRouter("/", orderRoutes.router(vertx));

        vertx.createHttpServer()
                .requestHandler(root)
                .listen(port)
                .onSuccess(s -> startPromise.complete())
                .onFailure(startPromise::fail);

        vertx.deployVerticle(new com.asml.email.EmailSenderVerticle());
    }
}