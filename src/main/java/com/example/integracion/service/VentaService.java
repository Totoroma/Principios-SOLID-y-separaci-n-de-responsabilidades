package com.example.integracion.service;

import com.example.integracion.dto.VentaNormalizada;
import com.example.integracion.dto.RespuestaEstadoOperacion;
import com.example.integracion.dto.RespuestaEstadoVenta;
import com.example.integracion.repository.RepositorioVentas;
import com.example.integracion.repository.RepositorioOutboxEstadisticas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class VentaService {

    private final RepositorioVentas repositorioVentas;
    private final ProcesadorOutboxStock procesadorOutboxStock;
    private final RepositorioOutboxEstadisticas repositorioOutboxEstadisticas;

    public VentaService(RepositorioVentas repositorioVentas, ProcesadorOutboxStock procesadorOutboxStock,
                        RepositorioOutboxEstadisticas repositorioOutboxEstadisticas) {
        this.repositorioVentas = repositorioVentas;
        this.procesadorOutboxStock = procesadorOutboxStock;
        this.repositorioOutboxEstadisticas = repositorioOutboxEstadisticas;
    }

    public void procesarVenta(VentaNormalizada venta) {
        if (repositorioVentas.registrarSiNueva(venta)) {
            procesadorOutboxStock.procesarSiguiente();
        }
    }

    public Optional<RespuestaEstadoVenta> consultarEstado(long ventaId) {
        return repositorioVentas.consultarEstado(ventaId).map(estado -> {
            ArrayList<RespuestaEstadoOperacion> operaciones = new ArrayList<>(estado.operaciones());
            repositorioOutboxEstadisticas.consultarEstado(ventaId).ifPresent(operaciones::add);
            return new RespuestaEstadoVenta(ventaId, operaciones);
        });
    }
}