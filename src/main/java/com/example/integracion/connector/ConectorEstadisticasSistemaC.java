package com.example.integracion.connector;

import com.example.integracion.dto.SolicitudEstadisticaVenta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("sistema-c")
public class ConectorEstadisticasSistemaC implements ConectorEstadisticas {

    private final RestClient clienteHttp;
    private final String rutaDestino;

    public ConectorEstadisticasSistemaC(
            RestClient.Builder builder,
            @Value("${sistema-c.base-url:http://localhost:4000}") String urlBase,
            @Value("${sistema-c.ventas-path:/api/estadisticas/ventas}") String rutaVentas) {
        this.clienteHttp = builder.baseUrl(urlBase).build();
        this.rutaDestino = rutaVentas;
    }

    @Override
    public void enviarVenta(SolicitudEstadisticaVenta venta) {
        clienteHttp.post()
                .uri(rutaDestino)
                .body(venta)
                .retrieve()
                .toBodilessEntity();
    }
}
