package com.example.apigateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtDecoderConfig {

  @Bean
  @ConditionalOnMissingBean
  ReactiveJwtDecoder reactiveJwtDecoder(JwtProperties properties) {
    if (!properties.enabled() || !properties.hasIssuerUri()) {
      return token ->
          reactor.core.publisher.Mono.error(
              new IllegalStateException("JWT authentication is not configured"));
    }

    return ReactiveJwtDecoders.fromIssuerLocation(properties.issuerUri());
  }
}
