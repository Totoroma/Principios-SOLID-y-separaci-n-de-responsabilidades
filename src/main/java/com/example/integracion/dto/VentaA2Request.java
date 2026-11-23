package com.example.integracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;
import java.util.List;

public record VentaA2Request(
        @NotBlank @Pattern(regexp = "\\d+") String id,
        @NotNull OffsetDateTime timestamp,
        @NotEmpty List<@Valid ProductoA2Request> items
) {
}
