package com.example.integracion.normalizer;

import com.example.integracion.dto.ProductoA2Request;
import com.example.integracion.dto.VentaA2Request;
import com.example.integracion.dto.VentaNormalizada;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NormalizadorVentaTest {

    private final NormalizadorVentaA2 normalizer = new NormalizadorVentaA2();

    @Test
    void normalizesA2SaleToCanonicalSale() {
        OffsetDateTime fecha = OffsetDateTime.parse("2026-09-24T16:20:35-03:00");
        VentaA2Request request = new VentaA2Request(
                "154872",
                fecha,
                List.of(new ProductoA2Request("7791234567890", 2))
        );

        VentaNormalizada venta = normalizer.normalizar(request);

        assertEquals(154872L, venta.ventaId());
        assertEquals(fecha, venta.fecha());
        assertEquals("A2", venta.proveedor());
        assertNull(venta.cajeroId());
        assertEquals("7791234567890", venta.productos().getFirst().codigo());
        assertEquals(2, venta.productos().getFirst().cantidad());
        assertNull(venta.productos().getFirst().precioUnitario());
    }
}
