# ADR 0001 — Versiones de la cadena de herramientas del prototipo

Fecha: 2026-10-16   Estado: propuesta (pasa a aceptada al aprobarse el pull request)

## Contexto

El prototipo debe compilar y ejecutar sus pruebas de forma reproducible en cualquier equipo, en
Docker y en integración continua. Al revisar el repositorio se encontró una combinación incoherente:

- `gradle/wrapper/gradle-wrapper.properties` apuntaba a Gradle 9.8.0, mientras que el plugin de Android
  declarado en `build.gradle.kts` es AGP 8.2.2, que pertenece a la serie de Gradle 8.x.
- `settings.gradle.kts` declaraba `kotlin("jvm") 2.4.21`, pero el módulo usa
  `org.jetbrains.kotlin.android` 1.9.22; el plugin declarado en settings no se usaba.
- El wrapper (`gradlew` y su `.jar`) no estaba en el repositorio.

## Opciones consideradas

1. Mantener Gradle 9.8.0 y actualizar AGP y Kotlin a versiones compatibles — implica migrar plugins,
   dependencias y revisar todo el módulo antes de tener un prototipo estable.
2. Fijar Gradle 8.5 con AGP 8.2.2, Kotlin 1.9.22 y JDK 17 — combinación coherente con lo ya declarado en el código.
3. Dejar el wrapper como estaba — la compilación depende de qué Gradle tenga cada integrante.

## Decisión

Se adopta la opción 2: JDK 17 (Temurin), Gradle 8.5, AGP 8.2.2, Kotlin 1.9.22, `compileSdk` 34.
La misma versión de Gradle se fija en el wrapper, el `Dockerfile` y el flujo de CI.
Se elimina el plugin `kotlin("jvm")` no utilizado de `settings.gradle.kts`.

## Consecuencias

- La compilación es reproducible en local, Docker y GitHub Actions con las mismas versiones.
- Subir de versión AGP o Gradle exigirá un nuevo ADR que reemplace a este.
- Hay que regenerar `gradlew`, `gradlew.bat` y `gradle-wrapper.jar` para Gradle 8.5 y subirlos al repositorio.
- No se podrán usar funciones exclusivas de AGP 9 / Gradle 9 durante el proyecto.
