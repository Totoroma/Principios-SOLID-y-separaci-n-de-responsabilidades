package com.example.integracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record MovimientoManualRequest(
        @NotBlank
        @Pattern(regexp = "ENTRADA|SALIDA", message = "Debe ser ENTRADA o SALIDA")
        String tipoMovimiento,
        @NotEmpty List<@Valid ProductoMovimientoRequest> productos
) {
}