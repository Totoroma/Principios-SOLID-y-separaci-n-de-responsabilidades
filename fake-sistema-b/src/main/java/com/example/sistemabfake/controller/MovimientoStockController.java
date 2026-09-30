package com.example.sistemabfake.controller;

import com.example.sistemabfake.dto.MovimientoStockRequest;
import com.example.sistemabfake.dto.ProductoRequest;
import com.example.sistemabfake.dto.ProductoStockResponse;
import com.example.sistemabfake.service.StockInventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/stock")
public class MovimientoStockController {

    private static final Logger logger = LoggerFactory.getLogger(MovimientoStockController.class);
    private final List<MovimientoStockRequest> movimientos = new CopyOnWriteArrayList<>();
    private final StockInventoryService inventoryService;
    private final String apiKey;
    private final boolean simularErrorInterno;

    public MovimientoStockController(
            StockInventoryService inventoryService,
            @Value("${fake-sistema-b.api-key:demo-key}") String apiKey,
            @Value("${fake-sistema-b.simulate-internal-error:false}") boolean simularErrorInterno) {
        this.inventoryService = inventoryService;
        this.apiKey = apiKey;
        this.simularErrorInterno = simularErrorInterno;
    }

    @PostMapping("/productos")
    public ResponseEntity<?> registrarProducto(
            @Valid @RequestBody ProductoRequest producto,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyRecibida) {
        ResponseEntity<ProblemDetail> authorizationError = validarApiKey(apiKeyRecibida);
        if (authorizationError != null) {
            return authorizationError;
        }

        inventoryService.registrarProducto(producto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductoStockResponse(producto.codigoProducto(), 0));
    }

    @PostMapping("/movimiento")
    public ResponseEntity<?> registrar(
            @Valid @RequestBody MovimientoStockRequest movimiento,
            @RequestHeader(value = "X-API-Key", required = false) String apiKeyRecibida) {
        ResponseEntity<ProblemDetail> authorizationError = validarApiKey(apiKeyRecibida);
        if (authorizationError != null) {
            return authorizationError;
        }

        if (simularErrorInterno) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                    "El Sistema B ficticio está configurado para simular un fallo interno.");
        }

        inventoryService.registrarMovimiento(movimiento);
        movimientos.add(movimiento);
        logger.info("Movimiento recibido: {}", movimiento);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/productos")
    public List<ProductoStockResponse> listarProductos() {
        return inventoryService.listarProductos();
    }

    @GetMapping("/movimientos")
    public List<MovimientoStockRequest> listarMovimientos() {
        return List.copyOf(movimientos);
    }

    private ResponseEntity<ProblemDetail> validarApiKey(String apiKeyRecibida) {
        if (apiKeyRecibida == null || apiKeyRecibida.isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "Autenticación requerida",
                    "Debe enviar la cabecera X-API-Key para registrar movimientos.");
        }
        if (!apiKey.equals(apiKeyRecibida)) {
            return error(HttpStatus.FORBIDDEN, "Acceso denegado",
                    "La clave API no es válida para registrar movimientos.");
        }
        return null;
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}