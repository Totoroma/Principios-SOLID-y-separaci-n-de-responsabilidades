package com.example.integracion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoVentaRequest(
        @NotBlank String codigo,
        @NotNull @DecimalMin(value = "0.01") Integer cantidad,
        @NotNull @DecimalMin(value = "0.0") BigDecimal precioUnitario
) {
}