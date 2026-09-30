package com.example.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("sistema-b")
class VentaToSistemaBIntegrationTest {

    private static final AtomicReference<String> movimientoRecibido = new AtomicReference<>();
    private static final AtomicReference<String> metodoRecibido = new AtomicReference<>();
    private static final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private static HttpServer sistemaB;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            sistemaB.start();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo iniciar la API ficticia del sistema B", exception);
        }

        registry.add("sistema-b.base-url", () -> "http://127.0.0.1:" + sistemaB.getAddress().getPort());
    }

    @BeforeEach
    void clearReceivedMovement() {
        movimientoRecibido.set(null);
        metodoRecibido.set(null);
        rutaRecibida.set(null);
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
}