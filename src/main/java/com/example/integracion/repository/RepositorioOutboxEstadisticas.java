package com.example.integracion.repository;

import com.example.integracion.dto.RespuestaEstadoOperacion;
import com.example.integracion.dto.SolicitudEstadisticaVenta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RepositorioOutboxEstadisticas {

    private final JdbcTemplate jdbcTemplate;

    public RepositorioOutboxEstadisticas(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Optional<OperacionOutboxEstadisticas> reclamarProximaPendiente(Instant ahora) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT venta_id FROM estadisticas_outbox "
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
                "UPDATE estadisticas_outbox SET estado = 'EN_PROCESO', intentos = intentos + 1, "
                        + "proximo_intento = NULL, procesando_desde = ?, actualizado_en = ? "
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
                "SELECT intentos FROM estadisticas_outbox WHERE venta_id = ?",
                Integer.class,
                ventaId
        );
        return Optional.of(new OperacionOutboxEstadisticas(cargarSolicitud(ventaId), intentos));
    }

    @Transactional
    public void marcarProcesada(long ventaId, Instant ahora) {
        jdbcTemplate.update(
                "UPDATE estadisticas_outbox SET estado = 'PROCESADO', proximo_intento = NULL, "
                        + "ultimo_error = NULL, procesado_en = ?, procesando_desde = NULL, actualizado_en = ? "
                        + "WHERE venta_id = ?",
                ahora.toEpochMilli(),
                ahora.toEpochMilli(),
                ventaId
        );
    }

    @Transactional
    public void programarReintento(long ventaId, Instant proximoIntento, String error) {
        jdbcTemplate.update(
                "UPDATE estadisticas_outbox SET estado = 'PENDIENTE', proximo_intento = ?, ultimo_error = ?, "
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
                "UPDATE estadisticas_outbox SET estado = 'FALLIDO', proximo_intento = NULL, "
                        + "ultimo_error = ?, procesando_desde = NULL, actualizado_en = ? WHERE venta_id = ?",
                limitarError(error),
                ahora,
                ventaId
        );
    }

    @Transactional
    public void liberarProcesosAbandonados(Instant anterioresA) {
        long ahora = Instant.now().toEpochMilli();
        jdbcTemplate.update(
                "UPDATE estadisticas_outbox SET estado = 'PENDIENTE', proximo_intento = ?, "
                        + "procesando_desde = NULL, actualizado_en = ? "
                        + "WHERE estado = 'EN_PROCESO' AND procesando_desde < ?",
                ahora,
                ahora,
                anterioresA.toEpochMilli()
        );
    }

    public Optional<RespuestaEstadoOperacion> consultarEstado(long ventaId) {
        List<RespuestaEstadoOperacion> estados = jdbcTemplate.query(
                "SELECT estado, intentos, ultimo_error, proximo_intento, procesado_en "
                        + "FROM estadisticas_outbox WHERE venta_id = ?",
                (resultSet, rowNumber) -> new RespuestaEstadoOperacion(
                        "C",
                        resultSet.getString("estado"),
                        resultSet.getInt("intentos"),
                        resultSet.getString("ultimo_error"),
                        instantOrNull(resultSet.getObject("proximo_intento", Long.class)),
                        instantOrNull(resultSet.getObject("procesado_en", Long.class))
                ),
                ventaId
        );
        return estados.stream().findFirst();
    }

    private SolicitudEstadisticaVenta cargarSolicitud(long ventaId) {
        return jdbcTemplate.queryForObject(
                "SELECT CAST(? AS BIGINT) AS venta_id, SUM(cantidad) AS cantidad_productos, "
                        + "CASE WHEN COUNT(*) = COUNT(precio_unitario) "
                        + "THEN SUM(cantidad * precio_unitario) ELSE NULL END AS importe_total "
                        + "FROM venta_productos WHERE venta_id = ?",
                (resultSet, rowNumber) -> new SolicitudEstadisticaVenta(
                        resultSet.getLong("venta_id"),
                        resultSet.getInt("cantidad_productos"),
                        resultSet.getBigDecimal("importe_total")
                ),
                ventaId,
                ventaId
        );
    }

    private Instant instantOrNull(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    private String limitarError(String error) {
        String detalle = error == null ? "Error de comunicación sin detalle" : error;
        return detalle.length() <= 1000 ? detalle : detalle.substring(0, 1000);
    }

    public record OperacionOutboxEstadisticas(SolicitudEstadisticaVenta venta, int intentos) {
    }
}
