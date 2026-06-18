package com.example.orderservice.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateOrderRequest(
    String orderId,
    @NotBlank String productName,
    @NotNull @Min(1) Integer quantity,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank @Email String customerEmail) {}
