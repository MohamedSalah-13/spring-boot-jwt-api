package com.demo.productapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "jane@example.com")
        @NotBlank @Size(max = 255)
        String email,

        @Schema(example = "S3cure-Passw0rd")
        @NotBlank @Size(max = 72)
        String password) {
}
