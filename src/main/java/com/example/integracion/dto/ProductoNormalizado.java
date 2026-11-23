package com.example.integracion.dto;

import java.math.BigDecimal;

public record ProductoNormalizado(
        String codigo,
        Integer cantidad,
        BigDecimal precioUnitario
) {
}
