package com.asml.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitHandler implements Handler<RoutingContext> {

    private final boolean enabled;
    private final int maxPerMinute;
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public RateLimitHandler(boolean enabled, int maxPerMinute) {
        this.enabled = enabled;
        this.maxPerMinute = Math.max(1, maxPerMinute);
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!enabled) {
            ctx.next();
            return;
        }

        String key = ctx.request().getHeader("X-Api-Key");
        if (key == null || key.isBlank()) key = "anonymous";

        int current = counters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();

        if (current > maxPerMinute) {
            ctx.response()
                    .setStatusCode(429)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"error\":\"RATE_LIMIT_EXCEEDED\"}");
            return;
        }

        ctx.next();
    }

    /** Call this once per minute from MainVerticle. */
    public void reset() {
        counters.clear();
    }
}
