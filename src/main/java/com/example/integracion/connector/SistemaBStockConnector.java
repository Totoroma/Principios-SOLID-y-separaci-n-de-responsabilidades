package com.example.integracion.connector;

import com.example.integracion.dto.MovimientoStockRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("sistema-b")
public class SistemaBStockConnector implements StockConnector {

    private final RestClient restClient;

    public SistemaBStockConnector(
            RestClient.Builder builder,
            @Value("${sistema-b.base-url:http://localhost:3000}") String baseUrl,
            @Value("${sistema-b.api-key:demo-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();
    }

    @Override
    public void registrarMovimiento(MovimientoStockRequest movimiento) {
        restClient
                .post()
                .uri("/api/stock/movimiento")
                .body(movimiento)
                .retrieve()
                .toBodilessEntity();
    }
}