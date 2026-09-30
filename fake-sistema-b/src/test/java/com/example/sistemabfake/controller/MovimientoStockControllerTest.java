package com.example.sistemabfake.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:fake-sistema-b-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class MovimientoStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearProducts() {
        jdbcTemplate.update("DELETE FROM stock_productos");
    }

    @Test
    void registersOnlyExplicitProductsAndAppliesPurchaseAndSale() throws Exception {
        mockMvc.perform(get("/api/stock/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/stock/productos")
                        .header("X-API-Key", "demo-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoProducto":"7791234567890"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadDisponible").value(0));

        postMovement("ENTRADA", "COMPRA-001", "7791234567890", 10)
                .andExpect(status().isNoContent());
        assertEquals(10, consultarStock("7791234567890"));

        postMovement("SALIDA", "VENTA-001", "7791234567890", 3)
                .andExpect(status().isNoContent());
        assertEquals(7, consultarStock("7791234567890"));

        String historyBody = mockMvc.perform(get("/api/stock/movimientos"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode history = objectMapper.readTree(historyBody);
        assertTrue(containsReference(history, "COMPRA-001"));
        assertTrue(containsReference(history, "VENTA-001"));
    }

    @Test
    void rejectsSaleWhenProductWasNeverRegistered() throws Exception {
        postMovement("SALIDA", "VENTA-UNKNOWN", "NO-REGISTRADO", 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Producto no encontrado"));
    }

    @Test
    void returnsConflictAndKeepsStockWhenQuantityExceedsAvailability() throws Exception {
        registerProduct("7791234567890");
        postMovement("ENTRADA", "COMPRA-002", "7791234567890", 4)
                .andExpect(status().isNoContent());

        postMovement("SALIDA", "VENTA-002", "7791234567890", 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Stock insuficiente"));

        assertEquals(4, consultarStock("7791234567890"));
    }

    @Test
    void returnsConflictWhenProductIsRegisteredTwice() throws Exception {
        registerProduct("7791234567890");

        mockMvc.perform(post("/api/stock/productos")
                        .header("X-API-Key", "demo-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoProducto":"7791234567890"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Producto ya registrado"));
    }

    @Test
    void returnsBadRequestWhenQuantityIsNotPositive() throws Exception {
        postMovement("SALIDA", "VENTA-INVALIDA", "7791234567890", 0)
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsForbiddenWhenApiKeyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/stock/movimiento")
                        .header("X-API-Key", "incorrecta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson("SALIDA", "VENTA-003", "7791234567890", 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsUnauthorizedWhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(post("/api/stock/movimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson("SALIDA", "VENTA-004", "7791234567890", 1)))
                .andExpect(status().isUnauthorized());
    }

    private void registerProduct(String code) throws Exception {
        mockMvc.perform(post("/api/stock/productos")
                        .header("X-API-Key", "demo-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("codigoProducto", code))))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions postMovement(
            String type, String reference, String code, int quantity) throws Exception {
        return mockMvc.perform(post("/api/stock/movimiento")
                .header("X-API-Key", "demo-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson(type, reference, code, quantity)));
    }

    private String movementJson(String type, String reference, String code, int quantity) {
        return """
                {
                  "tipoMovimiento": "%s",
                  "fecha": "2026-09-24T16:20:35-03:00",
                  "referencia": "%s",
                  "productos": [{"codigoProducto": "%s", "cantidad": %d}]
                }
                """.formatted(type, reference, code, quantity);
    }

    private int consultarStock(String code) throws Exception {
        String body = mockMvc.perform(get("/api/stock/productos"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode products = objectMapper.readTree(body);
        for (JsonNode product : products) {
            if (code.equals(product.get("codigoProducto").asText())) {
                return product.get("cantidadDisponible").asInt();
            }
        }
        throw new AssertionError("No existe el producto " + code + " en la respuesta");
    }

        private boolean containsReference(JsonNode movements, String reference) {
                for (JsonNode movement : movements) {
                        if (reference.equals(movement.get("referencia").asText())) {
                                return true;
                        }
                }
                return false;
        }
}