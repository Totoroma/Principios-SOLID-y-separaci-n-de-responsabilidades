# Documentación de la integración

Guía de los sistemas A/A2 → integración → Sistema B (stock) y Sistema C (estadísticas). Está orientada a Windows y PowerShell.

## Guías

- [Arquitectura](arquitectura.md)
- [Endpoints y formatos](endpoints.md)
- [Ejecución y pruebas](ejecucion-y-pruebas.md)
- [Compras y ventas desde PowerShell](movimientos-terminal.md)
- [Errores HTTP](errores.md)
- [Herramientas e instalación](herramientas.md)

## Árbol del proyecto

```text
integracion-stock/
├── .gitignore
├── Docs/
│   ├── README.md
│   ├── arquitectura.md
│   ├── endpoints.md
│   ├── errores.md
│   ├── ejecucion-y-pruebas.md
│   ├── herramientas.md
│   ├── movimientos-terminal.md
│   └── stock.ps1
├── fake-sistema-b/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/sistemabfake/
│       │   ├── FakeSistemaBApplication.java
│       │   ├── controller/
│       │   ├── dto/
│       │   └── service/
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── schema.sql
│       └── test/java/com/example/sistemabfake/
├── src/
│   ├── main/java/com/example/integracion/
│   │   ├── IntegracionApplication.java
│   │   ├── connector/          # adaptadores HTTP y simulados para B y C
│   │   ├── controller/        # entrada de A, A2 y consulta de estado
│   │   ├── dto/               # contratos externos y venta canónica
│   │   ├── mapper/
│   │   ├── normalizer/        # normalizadores de A y A2
│   │   ├── repository/        # persistencia y outboxes de B y C
│   │   └── service/           # procesadores independientes de cada outbox
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── schema.sql
│   └── test/java/com/example/integracion/
├── pom.xml
└── README.md
```

La integración guarda ventas y estados de entrega en `integracion-stock.mv.db`; el catálogo, los saldos y las referencias procesadas se guardan en `fake-sistema-b/sistema-b.mv.db`. Ambos archivos se excluyen por `.gitignore`. Los directorios `target/` se generan al compilar.