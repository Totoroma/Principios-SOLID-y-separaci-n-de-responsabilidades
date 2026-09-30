package com.example.integracion.mapper;

import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.dto.ProductoMovimientoRequest;
import com.example.integracion.dto.ProductoVentaRequest;
import com.example.integracion.dto.VentaRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovimientoStockMapper {

    public MovimientoStockRequest toMovimientoStock(VentaRequest venta) {
        List<ProductoMovimientoRequest> productos = venta.productos()
                .stream()
                .map(this::mapProducto)
                .toList();

        return new MovimientoStockRequest(
                "SALIDA",
                venta.fecha(),
                "VENTA-" + venta.ventaId(),
                productos
        );
    }

    private ProductoMovimientoRequest mapProducto(ProductoVentaRequest producto) {
        return new ProductoMovimientoRequest(
                producto.codigo(),
                producto.cantidad()
        );
    }
}