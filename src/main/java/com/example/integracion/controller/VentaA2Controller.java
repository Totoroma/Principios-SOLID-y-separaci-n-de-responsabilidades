package com.example.integracion.controller;

import com.example.integracion.dto.VentaA2Request;
import com.example.integracion.normalizer.NormalizadorVentaA2;
import com.example.integracion.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/a2/ventas")
public class VentaA2Controller {

    private final VentaService ventaService;
    private final NormalizadorVentaA2 normalizadorVentaA2;

    public VentaA2Controller(VentaService ventaService, NormalizadorVentaA2 normalizadorVentaA2) {
        this.ventaService = ventaService;
        this.normalizadorVentaA2 = normalizadorVentaA2;
    }

    @PostMapping
    public ResponseEntity<Void> recibirVenta(@Valid @RequestBody VentaA2Request venta) {
        ventaService.procesarVenta(normalizadorVentaA2.normalizar(venta));
        return ResponseEntity.ok().build();
    }
}
