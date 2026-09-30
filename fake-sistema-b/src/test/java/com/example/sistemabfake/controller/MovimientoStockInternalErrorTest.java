package com.example.sistemabfake.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.example.sistemabfake.service.StockInventoryService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoStockController.class)
@TestPropertySource(properties = "fake-sistema-b.simulate-internal-error=true")
class MovimientoStockInternalErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockInventoryService inventoryService;

    @Test
    void returnsInternalServerErrorWhenSimulationIsEnabled() throws Exception {
        mockMvc.perform(post("/api/stock/movimiento")
                        .header("X-API-Key", "demo-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoMovimiento": "SALIDA",
                                  "fecha": "2026-09-24T16:20:35-03:00",
                                  "referencia": "VENTA-154872",
                                  "productos": [{"codigoProducto": "7791234567890", "cantidad": 2}]
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Error interno"));
    }
}