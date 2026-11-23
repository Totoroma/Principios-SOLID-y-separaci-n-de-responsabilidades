package com.example.integracion.service;

import com.example.integracion.connector.ConectorEstadisticas;
import com.example.integracion.repository.RepositorioOutboxEstadisticas;
import com.example.integracion.repository.RepositorioOutboxEstadisticas.OperacionOutboxEstadisticas;
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
public class ProcesadorOutboxEstadisticas {

    private static final Logger registrador = LoggerFactory.getLogger(ProcesadorOutboxEstadisticas.class);

    private final RepositorioOutboxEstadisticas repositorio;
    private final ConectorEstadisticas conector;
    private final int maxIntentos;
    private final Duration demoraInicial;
    private final Duration demoraMaxima;
    private final Duration antiguedadMaximaProceso;

    public ProcesadorOutboxEstadisticas(
            RepositorioOutboxEstadisticas repositorio,
            ConectorEstadisticas conector,
            @Value("${integracion.outbox.max-attempts:5}") int maxIntentos,
            @Value("${integracion.outbox.initial-delay-ms:5000}") long demoraInicialMs,
            @Value("${integracion.outbox.max-delay-ms:300000}") long demoraMaximaMs,
            @Value("${integracion.outbox.stale-processing-ms:120000}") long procesoAbandonadoMs) {
        this.repositorio = repositorio;
        this.conector = conector;
        this.maxIntentos = maxIntentos;
        this.demoraInicial = Duration.ofMillis(demoraInicialMs);
        this.demoraMaxima = Duration.ofMillis(demoraMaximaMs);
        this.antiguedadMaximaProceso = Duration.ofMillis(procesoAbandonadoMs);
    }

    @Scheduled(fixedDelayString = "${integracion.statistics-outbox.poll-interval-ms:5000}")
    public void procesarPendientes() {
        repositorio.liberarProcesosAbandonados(Instant.now().minus(antiguedadMaximaProceso));
        for (int procesados = 0; procesados < 25; procesados++) {
            Optional<OperacionOutboxEstadisticas> siguiente =
                    repositorio.reclamarProximaPendiente(Instant.now());
            if (siguiente.isEmpty()) {
                return;
            }
            procesar(siguiente.get());
        }
    }

    private void procesar(OperacionOutboxEstadisticas operacion) {
        long ventaId = operacion.venta().ventaId();
        try {
            conector.enviarVenta(operacion.venta());
            repositorio.marcarProcesada(ventaId, Instant.now());
            registrador.info("Venta {} enviada al Sistema C en intento {}", ventaId, operacion.intentos());
        } catch (RestClientResponseException excepcion) {
            manejarErrorHttp(operacion, excepcion);
        } catch (ResourceAccessException excepcion) {
            manejarErrorTransitorio(operacion, errorDe(excepcion));
        }
    }

    private void manejarErrorHttp(OperacionOutboxEstadisticas operacion, RestClientResponseException excepcion) {
        String detalle = "HTTP " + excepcion.getStatusCode().value() + ": "
                + excepcion.getResponseBodyAsString();
        if (excepcion.getStatusCode().is4xxClientError() || operacion.intentos() >= maxIntentos) {
            repositorio.marcarFallida(operacion.venta().ventaId(), detalle);
            registrador.error("La venta {} quedó fallida en Sistema C: {}",
                    operacion.venta().ventaId(), detalle);
            return;
        }
        manejarErrorTransitorio(operacion, detalle);
    }

    private void manejarErrorTransitorio(OperacionOutboxEstadisticas operacion, String error) {
        String detalle = error == null ? "Error de comunicación sin detalle" : error;
        if (operacion.intentos() >= maxIntentos) {
            repositorio.marcarFallida(operacion.venta().ventaId(), detalle);
            registrador.error("La venta {} agotó sus intentos para Sistema C: {}",
                    operacion.venta().ventaId(), detalle);
            return;
        }

        long factor = 1L << Math.min(operacion.intentos() - 1, 20);
        Duration demora = demoraInicial.multipliedBy(factor);
        if (demora.compareTo(demoraMaxima) > 0) {
            demora = demoraMaxima;
        }
        Instant proximoIntento = Instant.now().plus(demora);
        repositorio.programarReintento(operacion.venta().ventaId(), proximoIntento, detalle);
        registrador.warn("No se pudo enviar la venta {} a Sistema C. Reintento {} para {}: {}",
                operacion.venta().ventaId(), operacion.intentos() + 1, proximoIntento, detalle);
    }

    private String errorDe(Exception excepcion) {
        return excepcion.getMessage() == null
                ? excepcion.getClass().getSimpleName()
                : excepcion.getMessage();
    }
}
