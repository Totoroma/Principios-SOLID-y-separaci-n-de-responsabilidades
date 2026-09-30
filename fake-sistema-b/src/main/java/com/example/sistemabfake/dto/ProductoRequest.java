package com.example.sistemabfake.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductoRequest(
        @NotBlank
        String codigoProducto
) {
}