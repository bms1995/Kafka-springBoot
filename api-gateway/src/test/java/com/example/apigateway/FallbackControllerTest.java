package com.example.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FallbackControllerTest {

  @Test
  void returnsServiceUnavailableFallbackResponse() {
    FallbackController controller = new FallbackController();

    var response = controller.getFallback("order-service").block();

    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().service()).isEqualTo("order-service");
    assertThat(response.getBody().message()).isEqualTo("Service temporarily unavailable");
    assertThat(response.getBody().timestamp()).isNotNull();
  }
}
