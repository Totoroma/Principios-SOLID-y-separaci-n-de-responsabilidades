package com.example.integracion.controller;

import com.example.integracion.dto.RespuestaEstadoVenta;
import com.example.integracion.service.VentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
public class VentaEstadoController {

    private final VentaService ventaService;

    public VentaEstadoController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping("/{ventaId}")
    public ResponseEntity<RespuestaEstadoVenta> consultarEstado(@PathVariable long ventaId) {
        return ventaService.consultarEstado(ventaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
