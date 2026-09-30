package com.example.sistemabfake.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record MovimientoStockRequest(
        @NotBlank
        String tipoMovimiento,
        @NotNull
        OffsetDateTime fecha,
        @NotBlank
        String referencia,
        @NotEmpty List<@Valid ProductoMovimientoRequest> productos
) {
}