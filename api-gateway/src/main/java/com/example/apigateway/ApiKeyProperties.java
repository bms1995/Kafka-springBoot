package com.example.apigateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.api-key")
public record ApiKeyProperties(
        boolean enabled,
        String headerName,
        String value
) {
}
