package com.example.integracion.normalizer;

import com.example.integracion.dto.VentaNormalizada;

public interface NormalizadorVenta<T> {

    VentaNormalizada normalizar(T venta);
}
