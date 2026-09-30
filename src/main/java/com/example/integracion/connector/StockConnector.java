package com.example.integracion.connector;

import com.example.integracion.dto.MovimientoStockRequest;

public interface StockConnector {

    void registrarMovimiento(MovimientoStockRequest movimiento);
}