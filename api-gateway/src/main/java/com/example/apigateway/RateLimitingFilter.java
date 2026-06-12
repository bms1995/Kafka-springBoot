package com.example.apigateway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitingFilter implements WebFilter, Ordered {

    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, ClientWindow> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitingFilter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimitingFilter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.enabled()) {
            return chain.filter(exchange);
        }

        ClientWindow window = windows.compute(clientKey(exchange.getRequest()), (key, current) -> {
            long now = clock.millis();
            if (current == null || now >= current.resetAtMillis()) {
                return new ClientWindow(now + properties.window().toMillis(), new AtomicInteger(1));
            }
            current.requests().incrementAndGet();
            return current;
        });

        int used = window.requests().get();
        int remaining = Math.max(properties.requestsPerWindow() - used, 0);
        exchange.getResponse().getHeaders().set(RATE_LIMIT_LIMIT_HEADER, String.valueOf(properties.requestsPerWindow()));
        exchange.getResponse().getHeaders().set(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));

        if (used > properties.requestsPerWindow()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private String clientKey(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private record ClientWindow(long resetAtMillis, AtomicInteger requests) {
    }
}
