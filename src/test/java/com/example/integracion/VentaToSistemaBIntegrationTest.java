package com.example.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.integracion.service.ProcesadorOutboxEstadisticas;
import com.example.integracion.service.ProcesadorOutboxStock;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"sistema-b", "sistema-c"})
class VentaToSistemaBIntegrationTest {

    private static final AtomicReference<String> movimientoRecibido = new AtomicReference<>();
    private static final AtomicReference<String> metodoRecibido = new AtomicReference<>();
    private static final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private static final AtomicInteger movimientosRecibidos = new AtomicInteger();
    private static final AtomicInteger respuestaSistemaB = new AtomicInteger(204);
    private static final AtomicReference<String> ventaEstadisticaRecibida = new AtomicReference<>();
    private static final AtomicInteger respuestaSistemaC = new AtomicInteger(204);
    private static HttpServer sistemaB;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProcesadorOutboxStock procesadorOutboxStock;

    @Autowired
    private ProcesadorOutboxEstadisticas procesadorOutboxEstadisticas;

    @DynamicPropertySource
    static void configureSistemaB(DynamicPropertyRegistry registry) {
        try {
            sistemaB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            sistemaB.createContext("/api/stock/movimiento", exchange -> {
                metodoRecibido.set(exchange.getRequestMethod());
                rutaRecibida.set(exchange.getRequestURI().getPath());
                movimientoRecibido.set(new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                ));
                movimientosRecibidos.incrementAndGet();
                int status = respuestaSistemaB.get();
                if (status == 204) {
                    exchange.sendResponseHeaders(status, -1);
                } else {
                    byte[] body = "Sistema B no disponible".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            sistemaB.createContext("/api/estadisticas/ventas", exchange -> {
                ventaEstadisticaRecibida.set(new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                ));
                int status = respuestaSistemaC.get();
                if (status == 204) {
                    exchange.sendResponseHeaders(status, -1);
                } else {
                    byte[] body = "Sistema C no disponible".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            sistemaB.start();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo iniciar la API ficticia del sistema B", exception);
        }

        registry.add("sistema-b.base-url", () -> "http://127.0.0.1:" + sistemaB.getAddress().getPort());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:idempotencia;DB_CLOSE_DELAY=-1");
        registry.add("integracion.outbox.poll-interval-ms", () -> "3600000");
        registry.add("integracion.outbox.initial-delay-ms", () -> "0");
        registry.add("integracion.outbox.max-delay-ms", () -> "0");
        registry.add("integracion.outbox.max-attempts", () -> "2");
        registry.add("integracion.outbox.stale-processing-ms", () -> "10000");
        registry.add("sistema-c.base-url", () -> "http://127.0.0.1:" + sistemaB.getAddress().getPort());
        registry.add("integracion.statistics-outbox.poll-interval-ms", () -> "3600000");
    }

    @BeforeEach
    void clearReceivedMovement() {
        jdbcTemplate.update("DELETE FROM stock_outbox");
        jdbcTemplate.update("DELETE FROM estadisticas_outbox");
        jdbcTemplate.update("DELETE FROM venta_productos");
        jdbcTemplate.update("DELETE FROM ventas");
        movimientoRecibido.set(null);
        metodoRecibido.set(null);
        rutaRecibida.set(null);
        movimientosRecibidos.set(0);
        respuestaSistemaB.set(204);
        respuestaSistemaC.set(204);
        ventaEstadisticaRecibida.set(null);
    }

    @AfterAll
    static void stopSistemaB() {
        if (sistemaB != null) {
            sistemaB.stop(0);
        }
    }

    @Test
    void receivesSaleAndPostsMappedMovementToSistemaB() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "ventaId": 154872,
                  "fecha": "2026-09-24T16:20:35-03:00",
                  "cajeroId": 42,
                  "productos": [
                    {"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50},
                    {"codigo": "7799876543210", "cantidad": 1, "precioUnitario": 3500.00}
                  ]
                }
                """, headers);

        var response = restTemplate.postForEntity("/api/ventas", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("POST", metodoRecibido.get());
        assertEquals("/api/stock/movimiento", rutaRecibida.get());
        assertNotNull(movimientoRecibido.get());

        JsonNode actual = objectMapper.readTree(movimientoRecibido.get());
        JsonNode expected = objectMapper.readTree("""
                {
                  "tipoMovimiento": "SALIDA",
                  "fecha": "2026-09-24T16:20:35-03:00",
                  "referencia": "VENTA-154872",
                  "productos": [
                    {"codigoProducto": "7791234567890", "cantidad": 2},
                    {"codigoProducto": "7799876543210", "cantidad": 1}
                  ]
                }
                """);

        assertEquals(expected, actual);

        procesadorOutboxEstadisticas.procesarPendientes();
        JsonNode statsPayload = objectMapper.readTree(ventaEstadisticaRecibida.get());
        assertEquals(154872, statsPayload.get("ventaId").asLong());
        assertEquals(3, statsPayload.get("cantidadProductos").asInt());
        assertEquals(6001.0, statsPayload.get("importeTotal").decimalValue().doubleValue());
    }

    @Test
    void processesAAndA2SaleOnlyOnceWhenTheyShareVentaId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> saleA = new HttpEntity<>("""
                {
                  "ventaId": 154872,
                  "fecha": "2026-09-24T16:20:35-03:00",
                  "cajeroId": 42,
                  "productos": [{"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50}]
                }
                """, headers);
        HttpEntity<String> saleA2 = new HttpEntity<>("""
                {
                  "id": "154872",
                  "timestamp": "2026-09-24T16:20:35-03:00",
                  "items": [{"sku": "7791234567890", "units": 2}]
                }
                """, headers);

        assertEquals(HttpStatus.OK,
                restTemplate.postForEntity("/api/ventas", saleA, String.class).getStatusCode());
        assertEquals(HttpStatus.OK,
                restTemplate.postForEntity("/api/a2/ventas", saleA2, String.class).getStatusCode());

        assertEquals(1, movimientosRecibidos.get());
    }

    @Test
    void keepsUnavailableSalePendingAndRetriesUntilSistemaBResponds() throws Exception {
        respuestaSistemaB.set(503);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(validSaleJson(), headers);

        var response = restTemplate.postForEntity("/api/ventas", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode pending = objectMapper.readTree(
                restTemplate.getForObject("/api/ventas/154872", String.class));
        assertEquals("PENDIENTE", pending.at("/operaciones/0/estado").asText());
        assertEquals(1, pending.at("/operaciones/0/intentos").asInt());

        respuestaSistemaB.set(204);
        procesadorOutboxStock.procesarPendientes();
        JsonNode current = objectMapper.readTree(
                restTemplate.getForObject("/api/ventas/154872", String.class));
        assertEquals("PROCESADO", current.at("/operaciones/0/estado").asText());
        assertEquals(2, movimientosRecibidos.get());
    }

    @Test
    void marksSaleFailedWhenTransientRetriesAreExhausted() throws Exception {
        respuestaSistemaB.set(503);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(validSaleJson(), headers);

        assertEquals(HttpStatus.OK,
                restTemplate.postForEntity("/api/ventas", request, String.class).getStatusCode());

        procesadorOutboxStock.procesarPendientes();
        JsonNode current = objectMapper.readTree(
                restTemplate.getForObject("/api/ventas/154872", String.class));
        assertEquals("FALLIDO", current.at("/operaciones/0/estado").asText());
        assertEquals(2, current.at("/operaciones/0/intentos").asInt());
    }

    @Test
    void systemCFailureDoesNotPreventSistemaBStockProcessing() throws Exception {
        respuestaSistemaC.set(503);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(validSaleJson(), headers);

        assertEquals(HttpStatus.OK,
                restTemplate.postForEntity("/api/ventas", request, String.class).getStatusCode());

        JsonNode stateAfterStock = objectMapper.readTree(
                restTemplate.getForObject("/api/ventas/154872", String.class));
        assertEquals("PROCESADO", stateAfterStock.at("/operaciones/0/estado").asText());
        assertEquals("PENDIENTE", stateAfterStock.at("/operaciones/1/estado").asText());

        procesadorOutboxEstadisticas.procesarPendientes();
        JsonNode stateAfterStatisticsFailure = objectMapper.readTree(
                restTemplate.getForObject("/api/ventas/154872", String.class));
        assertEquals("PROCESADO", stateAfterStatisticsFailure.at("/operaciones/0/estado").asText());
        assertEquals("FALLIDO", stateAfterStatisticsFailure.at("/operaciones/1/estado").asText());
        assertEquals(1, movimientosRecibidos.get());
    }

    @Test
    void sendsNullTotalForA2BecauseItDoesNotProvidePrices() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> saleA2 = new HttpEntity<>("""
                {
                  "id": "154872",
                  "timestamp": "2026-09-24T16:20:35-03:00",
                  "items": [{"sku": "7791234567890", "units": 2}]
                }
                """, headers);

        assertEquals(HttpStatus.OK,
                restTemplate.postForEntity("/api/a2/ventas", saleA2, String.class).getStatusCode());
        procesadorOutboxEstadisticas.procesarPendientes();

        JsonNode payload = objectMapper.readTree(ventaEstadisticaRecibida.get());
        assertEquals(2, payload.get("cantidadProductos").asInt());
        assertTrue(payload.get("importeTotal").isNull());
    }

    @Test
    void returnsNotFoundForUnknownSaleStatus() {
        assertEquals(HttpStatus.NOT_FOUND,
                restTemplate.getForEntity("/api/ventas/999999", String.class).getStatusCode());
    }

    @Test
    void receivesSimplifiedMovementAndGeneratesDateAndReference() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("""
                {
                  "tipoMovimiento": "ENTRADA",
                  "productos": [{"codigoProducto": "7791234567890", "cantidad": 20}]
                }
                """, headers);

        var response = restTemplate.postForEntity("/api/movimientos", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("POST", metodoRecibido.get());
        assertEquals("/api/stock/movimiento", rutaRecibida.get());

        JsonNode returned = objectMapper.readTree(response.getBody());
        JsonNode sentToSistemaB = objectMapper.readTree(movimientoRecibido.get());
        assertTrue(returned.get("referencia").asText().startsWith("ENTRADA-"));
        assertEquals("ENTRADA", returned.get("tipoMovimiento").asText());
        OffsetDateTime.parse(returned.get("fecha").asText());
        assertEquals(returned, sentToSistemaB);
    }

    private String validSaleJson() {
        return """
                {
                  "ventaId": 154872,
                  "fecha": "2026-09-24T16:20:35-03:00",
                  "cajeroId": 42,
                  "productos": [{"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50}]
                }
                """;
    }
}