package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void addsCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/order-1").build()
        );

        GatewayFilterChain chain = filteredExchange -> {
            String correlationId = filteredExchange.getRequest()
                    .getHeaders()
                    .getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertThat(correlationId).isNotBlank();
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank();
    }

    @Test
    void keepsIncomingCorrelationId() {
        String correlationId = "test-correlation-id";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders/order-1")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                        .build()
        );

        GatewayFilterChain chain = filteredExchange -> {
            assertThat(filteredExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                    .isEqualTo(correlationId);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(correlationId);
    }
}
