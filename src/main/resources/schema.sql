CREATE TABLE IF NOT EXISTS ventas (
    venta_id BIGINT PRIMARY KEY,
    fecha VARCHAR(64) NOT NULL,
    cajero_id BIGINT,
    proveedor VARCHAR(32) NOT NULL,
    recibida_en TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS venta_productos (
    venta_id BIGINT NOT NULL,
    producto_numero INTEGER NOT NULL,
    codigo VARCHAR(64) NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unitario DECIMAL(19, 2),
    PRIMARY KEY (venta_id, producto_numero),
    FOREIGN KEY (venta_id) REFERENCES ventas(venta_id)
);

CREATE TABLE IF NOT EXISTS stock_outbox (
    venta_id BIGINT PRIMARY KEY,
    estado VARCHAR(24) NOT NULL,
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento BIGINT,
    ultimo_error VARCHAR(1000),
    actualizado_en BIGINT NOT NULL,
    procesado_en BIGINT,
    procesando_desde BIGINT,
    FOREIGN KEY (venta_id) REFERENCES ventas(venta_id)
);

CREATE TABLE IF NOT EXISTS estadisticas_outbox (
    venta_id BIGINT PRIMARY KEY,
    estado VARCHAR(24) NOT NULL,
    intentos INTEGER NOT NULL DEFAULT 0,
    proximo_intento BIGINT,
    ultimo_error VARCHAR(1000),
    actualizado_en BIGINT NOT NULL,
    procesado_en BIGINT,
    procesando_desde BIGINT,
    FOREIGN KEY (venta_id) REFERENCES ventas(venta_id)
);
