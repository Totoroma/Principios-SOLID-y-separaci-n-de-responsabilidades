package com.example.sistemabfake.dto;

public record ProductoStockResponse(
        String codigoProducto,
        Integer cantidadDisponible
) {
}