# Principios SOLID y separación de responsabilidades

Proyecto Spring Boot que recibe ventas de los sistemas A y A2, las normaliza a un modelo común y coordina dos entregas independientes:

- **Sistema B / ERP:** registra la salida de stock.
- **Sistema C:** recibe datos de ventas para estadísticas.

Las ventas se guardan en H2 con `ventaId` único compartido entre A y A2. Las entregas tienen outboxes, estados y reintentos independientes. El simulador de B también evita aplicar dos veces un movimiento con la misma referencia.

Empezá por el [índice de documentación](Docs/README.md).

Para ejecutar la demo, revisar contratos y consultar estados, seguí [Ejecución y pruebas](Docs/ejecucion-y-pruebas.md) y [Endpoints](Docs/endpoints.md). Para registrar operaciones manuales desde PowerShell, seguí [Compras y ventas desde PowerShell](Docs/movimientos-terminal.md).