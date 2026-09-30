CREATE TABLE IF NOT EXISTS stock_productos (
    codigo_producto VARCHAR(64) PRIMARY KEY,
    cantidad_disponible INTEGER NOT NULL CHECK (cantidad_disponible >= 0)
);