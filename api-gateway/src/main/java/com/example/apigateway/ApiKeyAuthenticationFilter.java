package com.example.apigateway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@EnableConfigurationProperties({ApiKeyProperties.class, JwtProperties.class})
public class ApiKeyAuthenticationFilter implements WebFilter, Ordered {

  private final ApiKeyProperties properties;
  private final JwtProperties jwtProperties;
  private final ReactiveJwtDecoder jwtDecoder;

  @Autowired
  public ApiKeyAuthenticationFilter(
      ApiKeyProperties properties,
      JwtProperties jwtProperties,
      ObjectProvider<ReactiveJwtDecoder> jwtDecoder) {
    this(properties, jwtProperties, jwtDecoder.getIfAvailable());
  }

  ApiKeyAuthenticationFilter(
      ApiKeyProperties properties, JwtProperties jwtProperties, ReactiveJwtDecoder jwtDecoder) {
    this.properties = properties;
    this.jwtProperties = jwtProperties;
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!exchange.getRequest().getPath().value().startsWith("/api/")) {
      return chain.filter(exchange);
    }

    if (!properties.enabled() && !jwtProperties.enabled()) {
      return chain.filter(exchange);
    }

    String apiKey = exchange.getRequest().getHeaders().getFirst(properties.headerName());
    if (properties.enabled() && properties.value().equals(apiKey)) {
      return chain.filter(exchange);
    }

    String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
    if (jwtProperties.enabled() && authorization != null && authorization.startsWith("Bearer ")) {
      if (jwtDecoder == null) {
        return unauthorized(exchange);
      }

      String token = authorization.substring("Bearer ".length());
      return jwtDecoder
          .decode(token)
          .flatMap(jwt -> chain.filter(exchange))
          .onErrorResume(ex -> unauthorized(exchange));
    }

    return unauthorized(exchange);
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }
}
