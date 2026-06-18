package com.example.apigateway;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @GetMapping("/{serviceName}")
  public Mono<ResponseEntity<FallbackResponse>> getFallback(@PathVariable String serviceName) {
    return unavailable(serviceName);
  }

  @PostMapping("/{serviceName}")
  public Mono<ResponseEntity<FallbackResponse>> postFallback(@PathVariable String serviceName) {
    return unavailable(serviceName);
  }

  private Mono<ResponseEntity<FallbackResponse>> unavailable(String serviceName) {
    return Mono.just(
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                new FallbackResponse(
                    Instant.now(), serviceName, "Service temporarily unavailable")));
  }

  public record FallbackResponse(Instant timestamp, String service, String message) {}
}
