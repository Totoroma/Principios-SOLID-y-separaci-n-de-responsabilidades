package com.example.integracion.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record VentaNormalizada(
        Long ventaId,
        OffsetDateTime fecha,
        Long cajeroId,
        String proveedor,
        List<ProductoNormalizado> productos
) {
}
