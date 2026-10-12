package com.demo.productapi.product;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @Schema(example = "Mechanical Keyboard")
        @NotBlank @Size(max = 150)
        String name,

        @Schema(example = "Hot-swappable switches, USB-C")
        @Size(max = 2000)
        String description,

        @Schema(example = "79.90")
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @Schema(example = "25")
        @NotNull @PositiveOrZero
        Integer stock) {
}
