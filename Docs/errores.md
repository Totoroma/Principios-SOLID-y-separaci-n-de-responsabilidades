# Errores HTTP

El Sistema B ficticio devuelve `application/problem+json` con `status`, `title` y `detail`. La integración pasa al cliente el estado y el mensaje de B. Si B no es accesible, responde `502 Bad Gateway`.

| Estado | Causa | Prueba |
| --- | --- | --- |
| `400` | JSON o campos inválidos; cantidad menor que uno | Enviar un movimiento con cantidad `0`. |
| `401` | Falta `X-API-Key` | Hacer POST directo a B sin esa cabecera. |
| `403` | API key incorrecta | Configurar la integración con una clave distinta a la de B. |
| `404` | Producto no registrado | Enviar un código no incluido en `GET /api/stock/productos`. |
| `409` | Producto duplicado o stock insuficiente | Repetir un alta o intentar una salida mayor al saldo. |
| `500` | Fallo interno simulado | Iniciar B con la variable indicada abajo. |
| `502` | B está apagado o inaccesible | Detener B y enviar un movimiento por la integración. |

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

Reiniciar cada proceso sin su variable de error para volver al funcionamiento normal. Las variables solo aplican a procesos iniciados desde esa terminal.