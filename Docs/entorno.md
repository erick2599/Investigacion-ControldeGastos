# Documentación del Entorno de Ejecución

Este documento especifica el entorno contenedorizado mediante Docker para la compilación, pruebas y verificación del proyecto de investigación **Control de Gastos** (Android / Gradle).

## 1. Requisitos Previos
- Docker Desktop (v24.0 o superior) o Docker Engine en ejecución.
- Git instalado.

## 2. Componentes del Entorno
- **Base:** Eclipse Temurin JDK 17 (entorno base para Gradle y Android SDK).
- **Herramienta de compilación:** Gradle Wrapper incluido en el proyecto (`gradlew`).

## 3. Instrucciones de Despliegue y Ejecución

### Levantar el contenedor y verificar la compilación
Desde la raíz del proyecto, ejecutar:

```bash
docker compose up --build