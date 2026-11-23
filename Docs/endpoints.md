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

Éxito: `200 OK` cuando la venta quedó guardada en la integración. El procesamiento de B y C es independiente; un error transitorio deja la operación pendiente y se reintenta. Los rechazos permanentes o el agotamiento de intentos se reflejan como `FALLIDO` en la consulta de estado; no cambian la respuesta de recepción ya confirmada. Validación del JSON: `400`.

## Integración: consultar el estado de una venta

`GET http://localhost:8080/api/ventas/154872`

Devuelve `404` si la venta no fue recibida. Si existe, informa el estado de cada destino:

```json
{
  "ventaId": 154872,
  "operaciones": [
    {
      "destino": "B",
      "estado": "PROCESADO",
      "intentos": 1,
      "ultimoError": null,
      "proximoIntento": null,
      "procesadoEn": "2026-09-24T19:20:35Z"
    },
    {
      "destino": "C",
      "estado": "PENDIENTE",
      "intentos": 2,
      "ultimoError": "Connection refused",
      "proximoIntento": "2026-09-24T19:20:45Z",
      "procesadoEn": null
    }
  ]
}
```

Los estados de la operación con B son `PENDIENTE`, `EN_PROCESO`, `PROCESADO` y `FALLIDO`. Se reintentan errores de comunicación y respuestas HTTP 5xx, con demora exponencial limitada; los rechazos HTTP 4xx pasan a `FALLIDO`.

La respuesta también incluye la operación con C. El estado de C y sus reintentos son independientes de B. Estados posibles: `PENDIENTE`, `EN_PROCESO`, `PROCESADO` y `FALLIDO`.

## Integración: recibir una venta del sistema A2

`POST http://localhost:8080/api/a2/ventas`

```json
{
  "id": "154872",
  "timestamp": "2026-09-24T16:20:35-03:00",
  "items": [
    {"sku": "7791234567890", "units": 2}
  ]
}
```

A2 se normaliza al mismo modelo interno que usa el Sistema A antes de procesar la venta. Ambos comparten el espacio global de `ventaId`.
Si llega un `ventaId` que ya fue recibido desde cualquiera de los dos sistemas, la integración conserva la primera venta y no vuelve a procesar ni a enviar la segunda. Actualmente no valida si el contenido de la solicitud repetida coincide con la original.

## Integración: notificación de estadísticas al Sistema C

Para cada venta nueva, la integración envía de forma independiente:

```json
{
  "ventaId": 154872,
  "cantidadProductos": 3,
  "importeTotal": 6001.00
}
```

`cantidadProductos` es la suma de las unidades de todos los artículos. `importeTotal` se calcula con cantidad por precio unitario. Si la venta viene de A2, cuyo contrato no incluye precios, el importe se envía como `null`. Los fallos de C se reintentan sin bloquear ni revertir el movimiento de stock de B.

Al activar el perfil `sistema-c`, configurar `sistema-c.base-url` y opcionalmente `sistema-c.ventas-path` en `application.properties`. La ruta predeterminada es `/api/estadisticas/ventas`. Para usar los conectores HTTP reales de ambos sistemas, iniciar con los perfiles `sistema-b,sistema-c`.

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
