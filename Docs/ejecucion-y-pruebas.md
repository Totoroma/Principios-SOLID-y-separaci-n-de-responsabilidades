# Ejecución y pruebas

Usá PowerShell desde la raíz del proyecto. En la terminal integrada de VS Code no escribas `powershell`: ya estás en esa shell. Si `mvn` no está en `PATH`, definí una vez la ruta local:

```powershell
$Maven = "$HOME\.maven\maven-3.9.16\bin\mvn.cmd"
```

## Pruebas automatizadas

```powershell
& $Maven test
& $Maven -f ".\fake-sistema-b\pom.xml" test
```

La integración prueba mapper, endpoints, conectores y flujo HTTP. El simulador prueba validaciones, entradas, salidas y persistencia H2.

## Iniciar la demo

Abrí dos terminales en la raíz. Los comandos quedan ejecutándose en primer plano; dejá ambas terminales abiertas.

Terminal 1, Sistema B ficticio:

```powershell
Set-Location ".\fake-sistema-b"
$Maven = "$HOME\.maven\maven-3.9.16\bin\mvn.cmd"
& $Maven spring-boot:run
```

Terminal 2, integración con el conector HTTP:

```powershell
$Maven = "$HOME\.maven\maven-3.9.16\bin\mvn.cmd"
& $Maven "-Dspring-boot.run.profiles=sistema-b" spring-boot:run
```

Esperá el mensaje de inicio en las dos terminales. Si el prompt vuelve, revisá el error: el servidor no quedó activo.

Para registrar productos y hacer compras/ventas manuales desde PowerShell, seguí [Compras y ventas desde PowerShell](movimientos-terminal.md). El contrato de la notificación real del POS está en [Endpoints](endpoints.md); los errores, en [Errores HTTP](errores.md).