package com.example.sistemabfake.service;

import com.example.sistemabfake.dto.MovimientoStockRequest;
import com.example.sistemabfake.dto.ProductoRequest;
import com.example.sistemabfake.dto.ProductoStockResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class StockInventoryService {

    private final JdbcTemplate jdbcTemplate;

    public StockInventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductoStockResponse> listarProductos() {
        return jdbcTemplate.query(
                "SELECT codigo_producto, cantidad_disponible FROM stock_productos ORDER BY codigo_producto",
                (resultSet, rowNumber) -> new ProductoStockResponse(
                        resultSet.getString("codigo_producto"),
                        resultSet.getInt("cantidad_disponible")
                )
        );
    }

    @Transactional
    public void registrarProducto(ProductoRequest producto) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO stock_productos (codigo_producto, cantidad_disponible) VALUES (?, 0)",
                    producto.codigoProducto()
            );
        } catch (DuplicateKeyException exception) {
            throw new StockOperationException(409, "Producto ya registrado",
                    "El producto " + producto.codigoProducto() + " ya existe en el catálogo.");
        }
    }

    @Transactional
    public void registrarMovimiento(MovimientoStockRequest movimiento) {
        if (!"SALIDA".equals(movimiento.tipoMovimiento())
                && !"ENTRADA".equals(movimiento.tipoMovimiento())) {
            throw new StockOperationException(400, "Tipo de movimiento inválido",
                    "Solo se permiten movimientos de tipo ENTRADA o SALIDA.");
        }

        Map<String, Integer> cantidades = new TreeMap<>();
        movimiento.productos().forEach(producto ->
                cantidades.merge(producto.codigoProducto(), producto.cantidad(), Integer::sum));

        Map<String, Integer> existenciasActuales = new TreeMap<>();
        for (String codigo : cantidades.keySet()) {
            List<Integer> resultado = jdbcTemplate.query(
                    "SELECT cantidad_disponible FROM stock_productos WHERE codigo_producto = ? FOR UPDATE",
                    (resultSet, rowNumber) -> resultSet.getInt("cantidad_disponible"),
                    codigo
            );
            if (resultado.isEmpty()) {
                throw new StockOperationException(404, "Producto no encontrado",
                        "El producto " + codigo + " no está registrado en el catálogo.");
            }
            existenciasActuales.put(codigo, resultado.getFirst());
        }

        if ("SALIDA".equals(movimiento.tipoMovimiento())) {
            for (Map.Entry<String, Integer> entry : cantidades.entrySet()) {
                int disponible = existenciasActuales.get(entry.getKey());
                if (entry.getValue() > disponible) {
                    throw new StockOperationException(409, "Stock insuficiente",
                            "El producto " + entry.getKey() + " tiene " + disponible
                                    + " unidades disponibles y se solicitaron " + entry.getValue() + ".");
                }
            }
        }

        int factor = "ENTRADA".equals(movimiento.tipoMovimiento()) ? 1 : -1;
        cantidades.forEach((codigo, cantidad) -> jdbcTemplate.update(
                "UPDATE stock_productos SET cantidad_disponible = ? WHERE codigo_producto = ?",
                existenciasActuales.get(codigo) + factor * cantidad,
                codigo
        ));
    }
}