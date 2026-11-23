package com.example.integracion.service;

import com.example.integracion.connector.ConectorStock;
import com.example.integracion.dto.MovimientoManualRequest;
import com.example.integracion.dto.MovimientoStockRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class MovimientoManualService {

    private final ConectorStock conectorStock;

    public MovimientoManualService(ConectorStock conectorStock) {
        this.conectorStock = conectorStock;
    }

    public MovimientoStockRequest procesar(MovimientoManualRequest request) {
        String referencia = request.tipoMovimiento() + "-" + UUID.randomUUID();
        MovimientoStockRequest movimiento = new MovimientoStockRequest(
                request.tipoMovimiento(),
                OffsetDateTime.now(ZoneId.systemDefault()),
                referencia,
                request.productos()
        );

        conectorStock.registrarMovimiento(movimiento);
        return movimiento;
    }
}