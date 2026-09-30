package com.example.integracion.controller;

import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.dto.ProductoMovimientoRequest;
import com.example.integracion.service.MovimientoManualService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoManualController.class)
class MovimientoManualControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovimientoManualService movimientoManualService;

    @Test
    void acceptsMinimalMovementAndReturnsGeneratedFields() throws Exception {
        when(movimientoManualService.procesar(any())).thenReturn(new MovimientoStockRequest(
                "ENTRADA",
                OffsetDateTime.parse("2026-09-30T10:00:00-03:00"),
                "ENTRADA-123",
                List.of(new ProductoMovimientoRequest("7791234567890", 8))
        ));

        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoMovimiento": "ENTRADA",
                                  "productos": [{"codigoProducto": "7791234567890", "cantidad": 8}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$.referencia").value("ENTRADA-123"))
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.productos[0].cantidad").value(8));

        verify(movimientoManualService).procesar(any());
    }

    @Test
    void rejectsUnsupportedMovementType() throws Exception {
        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoMovimiento": "AJUSTE",
                                  "productos": [{"codigoProducto": "7791234567890", "cantidad": 8}]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsQuantityLessThanOne() throws Exception {
        mockMvc.perform(post("/api/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoMovimiento": "SALIDA",
                                  "productos": [{"codigoProducto": "7791234567890", "cantidad": 0}]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}