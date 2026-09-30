# Herramientas e instalación

## Requisitos

- **JDK 21**: versión declarada por ambos proyectos en sus `pom.xml` (`java.version=21`).
- **Apache Maven 3.9+**: compila, ejecuta pruebas y levanta las dos aplicaciones.
- **Conexión a Internet en la primera compilación**: Maven descarga dependencias desde Maven Central.
- **PowerShell** para los comandos de esta guía. Postman es opcional; se puede probar con `Invoke-RestMethod`.

No se requiere instalar un servidor de base de datos, Docker ni Postman. El Sistema B usa H2 embebido, que Maven descarga con las dependencias.

## Comprobar instalaciones

```powershell
java -version
mvn -version
```

La máquina usada durante el desarrollo tiene Maven en `$HOME\.maven\maven-3.9.16\bin\mvn.cmd`. Si `mvn` no está en `PATH`, usar la ruta completa en los comandos, como en [ejecución y pruebas](ejecucion-y-pruebas.md).

## Instalar en Windows

Con Windows Package Manager (`winget`):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
```

Después, cerrar y volver a abrir PowerShell y verificar con `java -version` y `mvn -version`. Si el comando Maven no se reconoce, descargar Maven desde [maven.apache.org](https://maven.apache.org/download.cgi), extraerlo y agregar su carpeta `bin` al `PATH`.

## Librerías y herramientas del proyecto

Maven instala automáticamente las dependencias descritas en los dos `pom.xml`:

- **Spring Boot 3.5.5 / Spring Web**: endpoints REST y cliente HTTP `RestClient`.
- **Jakarta Bean Validation**: validación de DTOs con `@Valid` y restricciones.
- **Spring JDBC y H2**: catálogo e inventario local persistido en un archivo; no se precargan datos de ejemplo.
- **Jackson**: convertir JSON a DTOs y viceversa; se configura para conservar el offset de fecha.
- **Spring Boot Test, JUnit 5 y Spring Test**: pruebas unitarias/HTTP; incluye `MockMvc` y `MockRestServiceServer`.
- **`HttpServer` del JDK**: API B temporal en la prueba de extremo a extremo.

En la primera ejecución, Maven puede tardar mientras descarga dependencias. No hace falta instalar esas librerías por separado.
