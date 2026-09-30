package com.example.integracion.controller;

import com.example.integracion.service.VentaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(VentaController.class)
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaService ventaService;

    @Test
    void acceptsSaleAndDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ventaId": 154872,
                                  "fecha": "2026-09-24T16:20:35-03:00",
                                  "cajeroId": 42,
                                  "productos": [
                                    {"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50},
                                    {"codigo": "7799876543210", "cantidad": 1, "precioUnitario": 3500.00}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        verify(ventaService).procesarVenta(any());
    }

    @Test
    void rejectsSaleWithInvalidProductQuantity() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ventaId": 154872,
                                  "fecha": "2026-09-24T16:20:35-03:00",
                                  "cajeroId": 42,
                                  "productos": [
                                    {"codigo": "7791234567890", "cantidad": 0, "precioUnitario": 1250.50}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(ventaService, never()).procesarVenta(any());
    }

      @Test
      void returnsSistemaBNotFoundStatusAndMessageToPos() throws Exception {
        HttpClientErrorException sistemaBError = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            HttpHeaders.EMPTY,
            """
                {"title":"Producto no encontrado","detail":"No existe el producto 7791234567890."}
                """.getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8
        );
        doThrow(sistemaBError).when(ventaService).procesarVenta(any());

        mockMvc.perform(post("/api/ventas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "ventaId": 154872,
                      "fecha": "2026-09-24T16:20:35-03:00",
                      "cajeroId": 42,
                      "productos": [{"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50}]
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Producto no encontrado"))
            .andExpect(jsonPath("$.detail").value("No existe el producto 7791234567890."));
      }

    @Test
    void returnsBadGatewayWhenSistemaBIsUnavailable() throws Exception {
        doThrow(new ResourceAccessException("Connection refused"))
                .when(ventaService).procesarVenta(any());

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ventaId": 154872,
                                  "fecha": "2026-09-24T16:20:35-03:00",
                                  "cajeroId": 42,
                                  "productos": [{"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50}]
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Sistema B no disponible"));
    }

                @Test
                void returnsSistemaBForbiddenStatusToPos() throws Exception {
              HttpClientErrorException sistemaBError = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                HttpHeaders.EMPTY,
                """
                  {"title":"Acceso denegado","detail":"La clave API no es válida."}
                  """.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
              );
              doThrow(sistemaBError).when(ventaService).procesarVenta(any());

              mockMvc.perform(post("/api/ventas")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validSaleJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso denegado"))
                .andExpect(jsonPath("$.detail").value("La clave API no es válida."));
                }

                @Test
                void returnsSistemaBInternalErrorStatusAndMessageToPos() throws Exception {
              HttpServerErrorException sistemaBError = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                """
                  {"title":"Error interno","detail":"El Sistema B ficticio simuló un fallo."}
                  """.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
              );
              doThrow(sistemaBError).when(ventaService).procesarVenta(any());

              mockMvc.perform(post("/api/ventas")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(validSaleJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(jsonPath("$.detail").value("El Sistema B ficticio simuló un fallo."));
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