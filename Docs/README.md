# Documentación de la integración

Guía de POS → integración → Sistema B ficticio. Está orientada a Windows y PowerShell.

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
│   │   ├── connector/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── mapper/MovimientoStockMapper.java
│   │   └── service/
│   ├── main/resources/application.properties
│   └── test/java/com/example/integracion/
├── pom.xml
└── README.md
```

El catálogo y los saldos se guardan en `fake-sistema-b/sistema-b.mv.db`, excluido por `.gitignore`. Los directorios `target/` se generan al compilar.