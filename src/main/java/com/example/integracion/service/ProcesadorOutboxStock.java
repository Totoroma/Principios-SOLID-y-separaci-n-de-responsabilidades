package com.example.integracion.service;

import com.example.integracion.connector.ConectorStock;
import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.repository.RepositorioVentas;
import com.example.integracion.repository.RepositorioVentas.OperacionOutboxStock;
import com.example.integracion.mapper.MovimientoStockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class ProcesadorOutboxStock {

    private static final Logger registrador = LoggerFactory.getLogger(ProcesadorOutboxStock.class);

    private final RepositorioVentas repositorioVentas;
    private final MovimientoStockMapper mapeadorMovimientoStock;
    private final ConectorStock conectorStock;
    private final int maxIntentos;
    private final Duration demoraInicial;
    private final Duration demoraMaxima;
    private final Duration antiguedadMaximaProceso;

    public ProcesadorOutboxStock(
            RepositorioVentas repositorioVentas,
            MovimientoStockMapper mapeadorMovimientoStock,
            ConectorStock conectorStock,
            @Value("${integracion.outbox.max-attempts:5}") int maxIntentos,
            @Value("${integracion.outbox.initial-delay-ms:5000}") long demoraInicialMs,
            @Value("${integracion.outbox.max-delay-ms:300000}") long demoraMaximaMs,
            @Value("${integracion.outbox.stale-processing-ms:120000}") long procesoAbandonadoMs) {
        this.repositorioVentas = repositorioVentas;
        this.mapeadorMovimientoStock = mapeadorMovimientoStock;
        this.conectorStock = conectorStock;
        this.maxIntentos = maxIntentos;
        this.demoraInicial = Duration.ofMillis(demoraInicialMs);
        this.demoraMaxima = Duration.ofMillis(demoraMaximaMs);
        this.antiguedadMaximaProceso = Duration.ofMillis(procesoAbandonadoMs);
    }

    public void procesarSiguiente() {
        Optional<OperacionOutboxStock> reclamada =
                repositorioVentas.reclamarProximaPendiente(Instant.now());
        if (reclamada.isPresent()) {
            procesar(reclamada.get());
        }
    }

    @Scheduled(fixedDelayString = "${integracion.outbox.poll-interval-ms:5000}")
    public void procesarPendientes() {
        repositorioVentas.liberarProcesosAbandonados(Instant.now().minus(antiguedadMaximaProceso));
        for (int procesados = 0; procesados < 25; procesados++) {
            Optional<OperacionOutboxStock> siguiente =
                    repositorioVentas.reclamarProximaPendiente(Instant.now());
            if (siguiente.isEmpty()) {
                return;
            }
            procesar(siguiente.get());
        }
    }

    private void procesar(OperacionOutboxStock operacion) {
        long ventaId = operacion.venta().ventaId();
        try {
            MovimientoStockRequest movimiento = mapeadorMovimientoStock.toMovimientoStock(operacion.venta());
            conectorStock.registrarMovimiento(movimiento);
            repositorioVentas.marcarProcesada(ventaId, Instant.now());
            registrador.info("Venta {} procesada por el Sistema B en intento {}",
                    ventaId, operacion.intentos());
        } catch (RestClientResponseException excepcion) {
            procesarErrorHttp(operacion, excepcion);
        } catch (ResourceAccessException excepcion) {
            programarReintento(operacion, errorDe(excepcion));
        }
    }

    private String errorDe(Exception excepcion) {
        return excepcion.getMessage() == null
                ? excepcion.getClass().getSimpleName()
                : excepcion.getMessage();
    }

    private void procesarErrorHttp(OperacionOutboxStock operacion, RestClientResponseException excepcion) {
        String detalle = "HTTP " + excepcion.getStatusCode().value() + ": "
                + excepcion.getResponseBodyAsString();
        if (excepcion.getStatusCode().is4xxClientError()
                || operacion.intentos() >= maxIntentos) {
            repositorioVentas.marcarFallida(operacion.venta().ventaId(), detalle);
            registrador.error("La venta {} quedó fallida en Sistema B: {}",
                    operacion.venta().ventaId(), detalle);
            return;
        }
        programarReintento(operacion, detalle);
    }

    private void programarReintento(OperacionOutboxStock operacion, String error) {
        String detalle = error == null ? "Error de comunicación sin detalle" : error;
        if (operacion.intentos() >= maxIntentos) {
            repositorioVentas.marcarFallida(operacion.venta().ventaId(), detalle);
            registrador.error("La venta {} agotó sus {} intentos: {}",
                    operacion.venta().ventaId(), maxIntentos, detalle);
            return;
        }

        long factor = 1L << Math.min(operacion.intentos() - 1, 20);
        Duration demora = demoraInicial.multipliedBy(factor);
        if (demora.compareTo(demoraMaxima) > 0) {
            demora = demoraMaxima;
        }
        Instant proximoIntento = Instant.now().plus(demora);
        repositorioVentas.programarReintento(operacion.venta().ventaId(), proximoIntento, detalle);
        registrador.warn("No se pudo enviar la venta {} a Sistema B. Reintento {} programado para {}: {}",
                operacion.venta().ventaId(), operacion.intentos() + 1,
                proximoIntento, detalle);
    }
}
