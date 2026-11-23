# Errores HTTP

El Sistema B ficticio devuelve `application/problem+json` con `status`, `title` y `detail`. Para operaciones manuales directas, el error HTTP de B se devuelve al cliente. Para ventas recibidas por A o A2, la integración ya guarda la venta antes de entregarla: los fallos de entrega se registran en la outbox y se consultan con `GET /api/ventas/{ventaId}`. Por eso un error de B o C no invalida el `200 OK` que confirma que la venta quedó guardada.

| Estado | Causa | Prueba |
| --- | --- | --- |
| `400` | JSON o campos inválidos; cantidad menor que uno | Enviar un movimiento con cantidad `0`. |
| `401` | Falta `X-API-Key` | Hacer POST directo a B sin esa cabecera. |
| `403` | API key incorrecta | Configurar la integración con una clave distinta a la de B. |
| `404` | Producto no registrado | Enviar un código no incluido en `GET /api/stock/productos`. |
| `409` | Producto duplicado, stock insuficiente o referencia repetida con otro contenido | Repetir un alta, exceder el saldo o reutilizar una referencia de movimiento con un payload diferente. Una repetición idéntica no vuelve a aplicar el stock. |
| `500` | Fallo interno simulado | Iniciar B con la variable indicada abajo. |
| `502` | B está apagado o inaccesible durante una operación manual síncrona | Detener B y enviar un movimiento manual por la integración. Las ventas quedan pendientes y se reintentan. |

Para ventas, los errores de comunicación y respuestas HTTP `5xx` se reintentan hasta el máximo configurado; los rechazos HTTP `4xx` de un destino pasan a `FALLIDO`. B y C mantienen estados separados. El límite y las demoras se configuran con `integracion.outbox.*` en `application.properties`.

## Simular errores

Los comandos siguientes usan `$Maven`, definido al inicio de [Ejecución y pruebas](ejecucion-y-pruebas.md). Para `403`, ejecutá el comando desde la raíz del proyecto; para `500`, desde `fake-sistema-b/` en la terminal de B.

Para probar `401`, llamar directamente a `POST http://localhost:3000/api/stock/movimiento` sin `X-API-Key`.

Para probar `403`, detener y reiniciar la integración con una clave incorrecta:

```powershell
$env:SISTEMA_B_API_KEY = "clave-incorrecta"
& $Maven "-Dspring-boot.run.profiles=sistema-b" spring-boot:run
```

Para probar `500`, reiniciar B con el modo de error habilitado:

```powershell
$env:FAKE_SISTEMA_B_SIMULATE_INTERNAL_ERROR = "true"
& $Maven spring-boot:run
```

En una venta recibida por `/api/ventas` o `/api/a2/ventas`, este fallo queda guardado y se puede observar en `GET /api/ventas/{ventaId}`; no se devuelve como error al sistema de origen después de confirmar la persistencia.

Reiniciar cada proceso sin su variable de error para volver al funcionamiento normal. Las variables solo aplican a procesos iniciados desde esa terminal.