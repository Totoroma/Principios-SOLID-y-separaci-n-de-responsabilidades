package com.example.integracion.connector;

import com.example.integracion.dto.MovimientoStockRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!sistema-b")
public class ConectorStockSimulado implements ConectorStock {

    private static final Logger registrador = LoggerFactory.getLogger(ConectorStockSimulado.class);

    @Override
    public void registrarMovimiento(MovimientoStockRequest movimiento) {
        registrador.info("Movimiento de stock simulado: {}", movimiento);
    }
}