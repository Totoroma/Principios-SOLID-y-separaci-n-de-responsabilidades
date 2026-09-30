package com.example.integracion.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MovimientoStockRequest(
        String tipoMovimiento,
        OffsetDateTime fecha,
        String referencia,
        List<ProductoMovimientoRequest> productos
) {
}