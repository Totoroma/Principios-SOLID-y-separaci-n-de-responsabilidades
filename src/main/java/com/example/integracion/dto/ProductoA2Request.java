package com.example.integracion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductoA2Request(
        @NotBlank String sku,
        @NotNull @DecimalMin(value = "0.01") Integer units
) {
}
