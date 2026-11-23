package com.example.integracion.normalizer;

import com.example.integracion.dto.ProductoNormalizado;
import com.example.integracion.dto.VentaA2Request;
import com.example.integracion.dto.VentaNormalizada;
import org.springframework.stereotype.Component;

@Component
public class NormalizadorVentaA2 implements NormalizadorVenta<VentaA2Request> {

    @Override
    public VentaNormalizada normalizar(VentaA2Request venta) {
        return new VentaNormalizada(
                Long.valueOf(venta.id()),
                venta.timestamp(),
                null,
                "A2",
                venta.items().stream()
                        .map(producto -> new ProductoNormalizado(
                                producto.sku(),
                                producto.units(),
                                null))
                        .toList()
        );
    }
}
