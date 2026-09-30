package com.example.integracion.connector;

import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.dto.ProductoMovimientoRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SistemaBStockConnectorTest {

    private static final String BASE_URL = "http://sistema-b.test";
    private static final String EXPECTED_BODY = """
            {
              "tipoMovimiento": "SALIDA",
              "fecha": "2026-09-24T16:20:35-03:00",
              "referencia": "VENTA-154872",
              "productos": [
                {"codigoProducto": "7791234567890", "cantidad": 2}
              ]
            }
            """;

    @Test
    void postsExpectedStockMovementToSistemaB() {
                RestClient.Builder builder = restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SistemaBStockConnector connector = new SistemaBStockConnector(builder, BASE_URL, "demo-key");

        server.expect(requestTo(BASE_URL + "/api/stock/movimiento"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "demo-key"))
                .andExpect(content().json(EXPECTED_BODY))
                .andRespond(withSuccess());

        connector.registrarMovimiento(movimiento());

        server.verify();
    }

    @Test
    void propagatesErrorWhenSistemaBReturnsServerFailure() {
                RestClient.Builder builder = restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SistemaBStockConnector connector = new SistemaBStockConnector(builder, BASE_URL, "demo-key");

        server.expect(requestTo(BASE_URL + "/api/stock/movimiento"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(RestClientResponseException.class,
                () -> connector.registrarMovimiento(movimiento()));

        server.verify();
    }

        @Test
        void propagatesForbiddenResponseFromSistemaB() {
                RestClient.Builder builder = restClientBuilder();
                MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
                SistemaBStockConnector connector = new SistemaBStockConnector(builder, BASE_URL, "demo-key");

                server.expect(requestTo(BASE_URL + "/api/stock/movimiento"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withForbiddenRequest());

                assertThrows(RestClientResponseException.class,
                                () -> connector.registrarMovimiento(movimiento()));

                server.verify();
        }

    private MovimientoStockRequest movimiento() {
        return new MovimientoStockRequest(
                "SALIDA",
                OffsetDateTime.parse("2026-09-24T16:20:35-03:00"),
                "VENTA-154872",
                List.of(new ProductoMovimientoRequest("7791234567890", 2))
        );
    }

        private RestClient.Builder restClientBuilder() {
                ObjectMapper objectMapper = new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

                return RestClient.builder()
                                .messageConverters(converters -> converters.add(
                                                0,
                                                new MappingJackson2HttpMessageConverter(objectMapper)
                                ));
        }
}