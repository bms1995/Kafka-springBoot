package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingFilterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void allowsRequestsWithinLimit() {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitProperties(true, 2, Duration.ofMinutes(1)),
                clock
        );
        AtomicInteger calls = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            calls.incrementAndGet();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange(), chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange(), chain)).verifyComplete();

        assertThat(calls).hasValue(2);
    }

    @Test
    void rejectsRequestsAboveLimit() {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitProperties(true, 1, Duration.ofMinutes(1)),
                clock
        );
        WebFilterChain chain = exchange -> Mono.empty();

        StepVerifier.create(filter.filter(exchange(), chain)).verifyComplete();
        MockServerWebExchange rejected = exchange();
        StepVerifier.create(filter.filter(rejected, chain)).verifyComplete();

        assertThat(rejected.getResponse().getStatusCode().value()).isEqualTo(429);
        assertThat(rejected.getResponse().getHeaders().getFirst(RateLimitingFilter.RATE_LIMIT_LIMIT_HEADER))
                .isEqualTo("1");
        assertThat(rejected.getResponse().getHeaders().getFirst(RateLimitingFilter.RATE_LIMIT_REMAINING_HEADER))
                .isEqualTo("0");
    }

    @Test
    void canBeDisabled() {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitProperties(false, 1, Duration.ofMinutes(1)),
                clock
        );
        AtomicInteger calls = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            calls.incrementAndGet();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange(), chain)).verifyComplete();
        StepVerifier.create(filter.filter(exchange(), chain)).verifyComplete();

        assertThat(calls).hasValue(2);
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 12345))
                        .build()
        );
    }
}
