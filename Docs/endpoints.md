# Endpoints y formatos

## Integración: recibir una venta

`POST http://localhost:8080/api/ventas`

Cabecera: `Content-Type: application/json`.

```json
{
  "ventaId": 154872,
  "fecha": "2026-09-24T16:20:35-03:00",
  "cajeroId": 42,
  "productos": [
    {"codigo": "7791234567890", "cantidad": 2, "precioUnitario": 1250.50},
    {"codigo": "7799876543210", "cantidad": 1, "precioUnitario": 3500.00}
  ]
}
```

Éxito: `200 OK`. Validación: `400`. Los errores de B conservan su estado.

## Integración: movimiento manual abreviado

`POST http://localhost:8080/api/movimientos`

Ruta manual para pruebas: acepta tipo y productos; la integración genera fecha y referencia (`ENTRADA-<UUID>` o `SALIDA-<UUID>`).

```json
{
  "tipoMovimiento": "ENTRADA",
  "productos": [
    {"codigoProducto": "7791234567890", "cantidad": 20}
  ]
}
```

Responde `200 OK` con el movimiento generado. Permite `ENTRADA` y `SALIDA`; el producto debe estar registrado en B. Para el comando abreviado de PowerShell, ver [Ejecución y pruebas](ejecucion-y-pruebas.md).

## Sistema B ficticio: registrar movimiento

`POST http://localhost:3000/api/stock/movimiento`

Cabeceras: `Content-Type: application/json`, `X-API-Key: demo-key`.

```json
{
  "tipoMovimiento": "SALIDA",
  "fecha": "2026-09-24T16:20:35-03:00",
  "referencia": "VENTA-154872",
  "productos": [
    {"codigoProducto": "7791234567890", "cantidad": 2},
    {"codigoProducto": "7799876543210", "cantidad": 1}
  ]
}
```

Acepta `ENTRADA` y `SALIDA`; responde `204 No Content` cuando registra el movimiento.

## Sistema B ficticio: registrar un producto del catálogo

`POST http://localhost:3000/api/stock/productos`

Cabeceras: `Content-Type: application/json` y `X-API-Key: demo-key`.

```json
{"codigoProducto":"7791234567890"}
```

Éxito: `201 Created`, saldo inicial `0`. Alta duplicada: `409`. El alta no agrega existencias.

## Sistema B ficticio: consultar historial

`GET http://localhost:3000/api/stock/movimientos`

Devuelve los movimientos aceptados durante la ejecución actual. No requiere API key.

## Sistema B ficticio: consultar existencias

`GET http://localhost:3000/api/stock/productos`

Devuelve productos y saldo actual. Una base nueva devuelve `[]`.

La configuración de la API key y la persistencia se explica en [Arquitectura](arquitectura.md). Las reglas de saldo insuficiente se detallan en [Errores HTTP](errores.md).
