CREATE TABLE IF NOT EXISTS stock_productos (
    codigo_producto VARCHAR(64) PRIMARY KEY,
    cantidad_disponible INTEGER NOT NULL CHECK (cantidad_disponible >= 0)
);

CREATE TABLE IF NOT EXISTS stock_movimientos_procesados (
    referencia VARCHAR(128) PRIMARY KEY,
    contenido VARCHAR(4000) NOT NULL
);