package com.example.integracion.service;

import com.example.integracion.connector.StockConnector;
import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.dto.VentaRequest;
import com.example.integracion.mapper.MovimientoStockMapper;
import org.springframework.stereotype.Service;

@Service
public class VentaService {

    private final MovimientoStockMapper mapper;
    private final StockConnector stockConnector;

    public VentaService(MovimientoStockMapper mapper, StockConnector stockConnector) {
        this.mapper = mapper;
        this.stockConnector = stockConnector;
    }

    public void procesarVenta(VentaRequest venta) {
        MovimientoStockRequest movimiento = mapper.toMovimientoStock(venta);
        stockConnector.registrarMovimiento(movimiento);
    }
}