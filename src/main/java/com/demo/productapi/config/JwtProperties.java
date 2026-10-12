package com.demo.productapi.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("product-api") String issuer,
        @DefaultValue("1h") Duration expiration) {
}
