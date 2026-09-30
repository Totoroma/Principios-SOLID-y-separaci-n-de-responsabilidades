# Compras y ventas desde PowerShell

Esta guía sirve para probar operaciones manuales. El POS real sigue usando `POST /api/ventas` con el contrato completo descrito en [Endpoints](endpoints.md).

## Antes de empezar

Dejá iniciados el Sistema B en `localhost:3000` y la integración con el perfil `sistema-b` en `localhost:8080`, como se explica en [Ejecución y pruebas](ejecucion-y-pruebas.md). Abrí una tercera terminal PowerShell en la raíz del proyecto.

## 1. Dar de alta un producto

La base comienza vacía. Registrá una vez cada código real que vayas a usar:

```powershell
$headers = @{ "X-API-Key" = "demo-key" }
$producto = @{ codigoProducto = "7791234567890" } | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:3000/api/stock/productos" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $producto
```

El producto queda con stock `0`. No vuelvas a registrarlo: un código duplicado devuelve `409`.

## 2. Cargar el comando corto

PowerShell puede bloquear la carga de archivos `.ps1`. Permití scripts solo en la terminal actual y cargá el helper:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
. .\Docs\stock.ps1
```

El cambio desaparece al cerrar esa terminal. Si una política de la organización lo impide, no la fuerces; consultá al administrador o usá el endpoint directo descrito en [Endpoints](endpoints.md).

## 3. Registrar una compra y una venta

Indicá el tipo, el código y la cantidad. `ENTRADA` suma unidades recibidas; `SALIDA` descuenta unidades vendidas:

```powershell
Send-StockMovement -Type ENTRADA -ProductCode "7791234567890" -Quantity 20
Send-StockMovement -Type SALIDA -ProductCode "7791234567890" -Quantity 2
```

Cada comando envía un movimiento individual. La integración genera la fecha y una referencia única, y devuelve el movimiento registrado. Para otro producto, repetí el comando con su código; para agrupar varios productos en un solo movimiento, usá el endpoint directo de B documentado en [Endpoints](endpoints.md).

## 4. Verificar existencias

```powershell
Invoke-RestMethod -Uri "http://localhost:3000/api/stock/productos"
```

El saldo esperado del ejemplo es `18`. Si una `SALIDA` supera el saldo disponible, el sistema responde `409` y no modifica el inventario. Los saldos persisten al reiniciar B.
