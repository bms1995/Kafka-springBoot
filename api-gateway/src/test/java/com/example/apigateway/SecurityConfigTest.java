package com.example.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.web.server.ServerHttpSecurity;

class SecurityConfigTest {

  @Test
  void createsPermissiveSecurityChainForGatewayFilters() {
    SecurityConfig config = new SecurityConfig();

    assertThat(config.securityWebFilterChain(ServerHttpSecurity.http())).isNotNull();
  }
}
