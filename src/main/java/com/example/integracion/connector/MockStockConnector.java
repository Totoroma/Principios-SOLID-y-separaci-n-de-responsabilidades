package com.example.integracion.connector;

import com.example.integracion.dto.MovimientoStockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!sistema-b")
public class MockStockConnector implements StockConnector {

    private static final Logger logger = LoggerFactory.getLogger(MockStockConnector.class);

    @Override
    public void registrarMovimiento(MovimientoStockRequest movimiento) {
        logger.info("Movimiento de stock simulado: {}", movimiento);
    }
}