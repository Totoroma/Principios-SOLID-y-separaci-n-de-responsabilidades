package com.example.integracion.controller;

import com.example.integracion.dto.VentaRequest;
import com.example.integracion.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<Void> recibirVenta(@Valid @RequestBody VentaRequest venta) {
        ventaService.procesarVenta(venta);
        return ResponseEntity.ok().build();
    }
}