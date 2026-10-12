package com.demo.productapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "jane@example.com")
        @NotBlank @Email @Size(max = 255)
        String email,

        // BCrypt only hashes the first 72 bytes, so longer passwords are rejected rather than truncated.
        @Schema(example = "S3cure-Passw0rd")
        @NotBlank @Size(min = 8, max = 72)
        String password) {
}
