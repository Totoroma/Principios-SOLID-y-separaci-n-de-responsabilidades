package com.example.sistemabfake.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductoMovimientoRequest(
        @NotBlank
        String codigoProducto,
        @NotNull @Min(1)
        Integer cantidad
) {
}