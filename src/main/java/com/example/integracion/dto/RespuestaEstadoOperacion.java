package com.example.integracion.dto;

import java.time.Instant;

public record RespuestaEstadoOperacion(
        String destino,
        String estado,
        int intentos,
        String ultimoError,
        Instant proximoIntento,
        Instant procesadoEn
) {
}
