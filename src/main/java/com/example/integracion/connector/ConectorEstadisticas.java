package com.example.integracion.connector;

import com.example.integracion.dto.SolicitudEstadisticaVenta;

public interface ConectorEstadisticas {

    void enviarVenta(SolicitudEstadisticaVenta venta);
}
