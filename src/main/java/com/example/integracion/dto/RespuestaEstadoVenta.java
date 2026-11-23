package com.example.integracion.dto;

import java.util.List;

public record RespuestaEstadoVenta(
        Long ventaId,
        List<RespuestaEstadoOperacion> operaciones
) {
}
