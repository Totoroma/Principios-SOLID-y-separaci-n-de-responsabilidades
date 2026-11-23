package com.example.integracion.connector;

import com.example.integracion.dto.MovimientoStockRequest;

public interface ConectorStock {

    void registrarMovimiento(MovimientoStockRequest movimiento);
}