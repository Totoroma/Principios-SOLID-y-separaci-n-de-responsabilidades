package com.example.integracion.mapper;

import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.dto.ProductoVentaRequest;
import com.example.integracion.dto.VentaRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovimientoStockMapperTest {

    private final MovimientoStockMapper mapper = new MovimientoStockMapper();

    @Test
    void mapsSaleToStockExitWithoutUnitPrice() {
        OffsetDateTime fecha = OffsetDateTime.parse("2026-09-24T16:20:35-03:00");
        VentaRequest venta = new VentaRequest(
                154872L,
                fecha,
                42L,
                List.of(new ProductoVentaRequest("7791234567890", 2, new BigDecimal("1250.50")))
        );

        MovimientoStockRequest movimiento = mapper.toMovimientoStock(venta);

        assertEquals("SALIDA", movimiento.tipoMovimiento());
        assertEquals(fecha, movimiento.fecha());
        assertEquals("VENTA-154872", movimiento.referencia());
        assertEquals("7791234567890", movimiento.productos().getFirst().codigoProducto());
        assertEquals(2, movimiento.productos().getFirst().cantidad());
    }
}