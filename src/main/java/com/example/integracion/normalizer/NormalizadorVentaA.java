package com.example.integracion.normalizer;

import com.example.integracion.dto.ProductoNormalizado;
import com.example.integracion.dto.VentaNormalizada;
import com.example.integracion.dto.VentaRequest;
import org.springframework.stereotype.Component;

@Component
public class NormalizadorVentaA implements NormalizadorVenta<VentaRequest> {

    @Override
    public VentaNormalizada normalizar(VentaRequest venta) {
        return new VentaNormalizada(
                venta.ventaId(),
                venta.fecha(),
                venta.cajeroId(),
                "A",
                venta.productos().stream()
                        .map(producto -> new ProductoNormalizado(
                                producto.codigo(),
                                producto.cantidad(),
                                producto.precioUnitario()))
                        .toList()
        );
    }
}
