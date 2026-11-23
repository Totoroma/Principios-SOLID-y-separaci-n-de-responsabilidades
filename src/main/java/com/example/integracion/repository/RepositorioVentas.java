package com.example.integracion.repository;

import com.example.integracion.dto.VentaNormalizada;
import com.example.integracion.dto.ProductoNormalizado;
import com.example.integracion.dto.RespuestaEstadoOperacion;
import com.example.integracion.dto.RespuestaEstadoVenta;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RepositorioVentas {

    private final JdbcTemplate jdbcTemplate;

    public RepositorioVentas(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean registrarSiNueva(VentaNormalizada venta) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ventas (venta_id, fecha, cajero_id, proveedor, recibida_en) VALUES (?, ?, ?, ?, ?)",
                    venta.ventaId(),
                    venta.fecha().toString(),
                    venta.cajeroId(),
                    venta.proveedor(),
                    Instant.now().toString()
            );
        } catch (DuplicateKeyException exception) {
            return false;
        }

        for (int indice = 0; indice < venta.productos().size(); indice++) {
            ProductoNormalizado producto = venta.productos().get(indice);
            jdbcTemplate.update(
                    "INSERT INTO venta_productos "
                            + "(venta_id, producto_numero, codigo, cantidad, precio_unitario) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    venta.ventaId(),
                    indice,
                    producto.codigo(),
                    producto.cantidad(),
                    producto.precioUnitario()
            );
        }

        long ahora = Instant.now().toEpochMilli();
        jdbcTemplate.update(
                "INSERT INTO stock_outbox (venta_id, estado, intentos, proximo_intento, actualizado_en) "
                        + "VALUES (?, 'PENDIENTE', 0, ?, ?)",
                venta.ventaId(),
                ahora,
                ahora
        );
        jdbcTemplate.update(
                "INSERT INTO estadisticas_outbox "
                        + "(venta_id, estado, intentos, proximo_intento, actualizado_en) "
                        + "VALUES (?, 'PENDIENTE', 0, ?, ?)",
                venta.ventaId(),
                ahora,
                ahora
        );

        return true;
    }

    @Transactional
    public Optional<OperacionOutboxStock> reclamarProximaPendiente(Instant ahora) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT venta_id FROM stock_outbox "
                        + "WHERE estado = 'PENDIENTE' AND proximo_intento <= ? "
                        + "ORDER BY proximo_intento, venta_id FETCH FIRST 1 ROW ONLY",
                (resultSet, rowNumber) -> resultSet.getLong("venta_id"),
                ahora.toEpochMilli()
        );
        if (ids.isEmpty()) {
            return Optional.empty();
        }

        long ventaId = ids.getFirst();
        int actualizados = jdbcTemplate.update(
                "UPDATE stock_outbox SET estado = 'EN_PROCESO', intentos = intentos + 1, "
                        + "proximo_intento = NULL, "
                        + "procesando_desde = ?, actualizado_en = ? "
                        + "WHERE venta_id = ? AND estado = 'PENDIENTE' AND proximo_intento <= ?",
                ahora.toEpochMilli(),
                ahora.toEpochMilli(),
                ventaId,
                ahora.toEpochMilli()
        );
        if (actualizados == 0) {
            return Optional.empty();
        }

        int intentos = jdbcTemplate.queryForObject(
                "SELECT intentos FROM stock_outbox WHERE venta_id = ?",
                Integer.class,
                ventaId
        );
        return Optional.of(new OperacionOutboxStock(cargarVenta(ventaId), intentos));
    }

    @Transactional
    public void marcarProcesada(long ventaId, Instant ahora) {
        jdbcTemplate.update(
                "UPDATE stock_outbox SET estado = 'PROCESADO', proximo_intento = NULL, ultimo_error = NULL, "
                        + "procesado_en = ?, procesando_desde = NULL, actualizado_en = ? WHERE venta_id = ?",
                ahora.toEpochMilli(),
                ahora.toEpochMilli(),
                ventaId
        );
    }

    @Transactional
    public void programarReintento(long ventaId, Instant proximoIntento, String error) {
        jdbcTemplate.update(
                "UPDATE stock_outbox SET estado = 'PENDIENTE', proximo_intento = ?, ultimo_error = ?, "
                        + "procesando_desde = NULL, actualizado_en = ? WHERE venta_id = ?",
                proximoIntento.toEpochMilli(),
                limitarError(error),
                Instant.now().toEpochMilli(),
                ventaId
        );
    }

    @Transactional
    public void marcarFallida(long ventaId, String error) {
        long ahora = Instant.now().toEpochMilli();
        jdbcTemplate.update(
                "UPDATE stock_outbox SET estado = 'FALLIDO', proximo_intento = NULL, ultimo_error = ?, "
                        + "procesando_desde = NULL, actualizado_en = ? WHERE venta_id = ?",
                limitarError(error),
                ahora,
                ventaId
        );
    }

    @Transactional
    public void liberarProcesosAbandonados(Instant anterioresA) {
        jdbcTemplate.update(
                "UPDATE stock_outbox SET estado = 'PENDIENTE', proximo_intento = ?, "
                        + "procesando_desde = NULL, actualizado_en = ? "
                        + "WHERE estado = 'EN_PROCESO' AND procesando_desde < ?",
                Instant.now().toEpochMilli(),
                Instant.now().toEpochMilli(),
                anterioresA.toEpochMilli()
        );
    }

    public Optional<RespuestaEstadoVenta> consultarEstado(long ventaId) {
        List<RespuestaEstadoOperacion> operaciones = jdbcTemplate.query(
                "SELECT estado, intentos, ultimo_error, proximo_intento, procesado_en "
                        + "FROM stock_outbox WHERE venta_id = ?",
                (resultSet, rowNumber) -> new RespuestaEstadoOperacion(
                        "B",
                        resultSet.getString("estado"),
                        resultSet.getInt("intentos"),
                        resultSet.getString("ultimo_error"),
                        instantOrNull(resultSet.getObject("proximo_intento", Long.class)),
                        instantOrNull(resultSet.getObject("procesado_en", Long.class))
                ),
                ventaId
        );
        if (operaciones.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RespuestaEstadoVenta(ventaId, operaciones));
    }

    private VentaNormalizada cargarVenta(long ventaId) {
        VentaRow venta = jdbcTemplate.queryForObject(
                "SELECT fecha, cajero_id, proveedor FROM ventas WHERE venta_id = ?",
                (resultSet, rowNumber) -> new VentaRow(
                        resultSet.getString("fecha"),
                        resultSet.getObject("cajero_id", Long.class),
                        resultSet.getString("proveedor")
                ),
                ventaId
        );
        List<ProductoNormalizado> productos = jdbcTemplate.query(
                "SELECT codigo, cantidad, precio_unitario FROM venta_productos "
                        + "WHERE venta_id = ? ORDER BY producto_numero",
                (resultSet, rowNumber) -> new ProductoNormalizado(
                        resultSet.getString("codigo"),
                        resultSet.getInt("cantidad"),
                        resultSet.getBigDecimal("precio_unitario")
                ),
                ventaId
        );
        return new VentaNormalizada(
                ventaId,
                java.time.OffsetDateTime.parse(venta.fecha()),
                venta.cajeroId(),
                venta.proveedor(),
                productos
        );
    }

    private Instant instantOrNull(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private String limitarError(String error) {
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }

    private record VentaRow(String fecha, Long cajeroId, String proveedor) {
    }

    public record OperacionOutboxStock(VentaNormalizada venta, int intentos) {
    }
}
