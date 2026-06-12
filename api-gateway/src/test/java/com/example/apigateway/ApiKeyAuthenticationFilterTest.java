package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticationFilterTest {

    @Test
    void rejectsApiRequestWithoutApiKey() {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
                new ApiKeyProperties(true, "X-API-Key", "secret")
        );

        MockServerWebExchange exchange = exchange("/api/orders");

        StepVerifier.create(filter.filter(exchange, noOpChain())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void allowsApiRequestWithValidApiKey() {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
                new ApiKeyProperties(true, "X-API-Key", "secret")
        );
        AtomicInteger calls = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            calls.incrementAndGet();
            return Mono.empty();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header("X-API-Key", "secret")
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(calls).hasValue(1);
    }

    @Test
    void doesNotProtectActuatorEndpoints() {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
                new ApiKeyProperties(true, "X-API-Key", "secret")
        );
        AtomicInteger calls = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            calls.incrementAndGet();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange("/actuator/health"), chain)).verifyComplete();

        assertThat(calls).hasValue(1);
    }

    @Test
    void canBeDisabled() {
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
                new ApiKeyProperties(false, "X-API-Key", "secret")
        );
        AtomicInteger calls = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            calls.incrementAndGet();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange("/api/orders"), chain)).verifyComplete();

        assertThat(calls).hasValue(1);
    }

    private MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }

    private WebFilterChain noOpChain() {
        return exchange -> Mono.empty();
    }
}
