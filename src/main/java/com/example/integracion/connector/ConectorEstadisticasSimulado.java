package com.example.integracion.connector;

import com.example.integracion.dto.SolicitudEstadisticaVenta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!sistema-c")
public class ConectorEstadisticasSimulado implements ConectorEstadisticas {

    private static final Logger registrador = LoggerFactory.getLogger(ConectorEstadisticasSimulado.class);

    @Override
    public void enviarVenta(SolicitudEstadisticaVenta venta) {
        registrador.info("Venta enviada al Sistema C simulado: {}", venta);
    }
}
