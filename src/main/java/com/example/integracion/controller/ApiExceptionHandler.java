package com.example.integracion.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final ObjectMapper objectMapper;

    public ApiExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ProblemDetail> handleSistemaBResponse(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String title = "Error del sistema B";
        String detail = "El sistema B rechazó el movimiento de stock.";

        try {
            JsonNode body = objectMapper.readTree(exception.getResponseBodyAsString());
            if (body.hasNonNull("title")) {
                title = body.get("title").asText();
            }
            if (body.hasNonNull("detail")) {
                detail = body.get("detail").asText();
            }
        } catch (IOException ignored) {
            if (!exception.getStatusText().isBlank()) {
                detail = exception.getStatusText();
            }
        }

        return problem(status, title, detail);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ProblemDetail> handleSistemaBUnavailable() {
        return problem(HttpStatus.BAD_GATEWAY, "Sistema B no disponible",
                "No se pudo establecer comunicación con el sistema B.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}