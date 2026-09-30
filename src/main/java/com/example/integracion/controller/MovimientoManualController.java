package com.example.integracion.controller;

import com.example.integracion.dto.MovimientoManualRequest;
import com.example.integracion.dto.MovimientoStockRequest;
import com.example.integracion.service.MovimientoManualService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoManualController {

    private final MovimientoManualService movimientoManualService;

    public MovimientoManualController(MovimientoManualService movimientoManualService) {
        this.movimientoManualService = movimientoManualService;
    }

    @PostMapping
    public ResponseEntity<MovimientoStockRequest> registrar(
            @Valid @RequestBody MovimientoManualRequest request) {
        return ResponseEntity.ok(movimientoManualService.procesar(request));
    }
}