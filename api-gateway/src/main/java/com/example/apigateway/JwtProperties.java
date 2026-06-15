package com.example.apigateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        boolean enabled,
        String issuerUri
) {
    public boolean hasIssuerUri() {
        return issuerUri != null && !issuerUri.isBlank();
    }
}
