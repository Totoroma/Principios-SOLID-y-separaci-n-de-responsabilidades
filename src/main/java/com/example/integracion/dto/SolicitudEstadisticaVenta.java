package com.example.integracion.dto;

import java.math.BigDecimal;

public record SolicitudEstadisticaVenta(
        Long ventaId,
        int cantidadProductos,
        BigDecimal importeTotal
) {
}
