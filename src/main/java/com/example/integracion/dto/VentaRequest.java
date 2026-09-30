package com.example.integracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record VentaRequest(
        @NotNull Long ventaId,
        @NotNull OffsetDateTime fecha,
        @NotNull Long cajeroId,
        @NotEmpty List<@Valid ProductoVentaRequest> productos
) {
}