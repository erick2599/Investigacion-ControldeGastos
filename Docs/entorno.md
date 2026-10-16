# Documentación del entorno

Entorno contenedorizado para compilar y probar el prototipo **Control de Gastos** (Android / Gradle).
Las celdas marcadas con `[COMPLETAR]` dependen del equipo real donde se ejecuten las mediciones
y deben llenarse con los datos de tu documento antes de la entrega.

## 1. Requisitos previos

- Docker Desktop (v24 o superior) o Docker Engine en ejecución.
- Git.

## 2. Componentes y versiones

| Componente | Versión | Dónde se fija |
|---|---|---|
| Imagen base (JDK) | Eclipse Temurin 17 (`eclipse-temurin:17-jdk`) | `Dockerfile` |
| Gradle | 8.5 | `Dockerfile`, `gradle/wrapper/gradle-wrapper.properties`, `.github/workflows/ci.yml` |
| Android Gradle Plugin | 8.2.2 | `build.gradle.kts` |
| Kotlin (plugin) | 1.9.22 | `build.gradle.kts` |
| Android command-line tools | build 11076708 | `Dockerfile` |
| Android SDK Platform | android-34 (`compileSdk`/`targetSdk` = 34, `minSdk` = 24) | `Dockerfile`, `app/build.gradle.kts` |
| Android build-tools | 34.0.0 | `Dockerfile` |

Versión exacta del JDK dentro de la imagen (anótala tras construirla):

```bash
docker run --rm --entrypoint java controldegastos-build:0.2 -version
```

Resultado: `[COMPLETAR]`

### Bibliotecas del módulo `app` (`app/build.gradle.kts`)

| Biblioteca | Versión |
|---|---|
| androidx.core:core-ktx | 1.12.0 |
| androidx.appcompat:appcompat | 1.6.1 |
| com.google.android.material:material | 1.11.0 |
| androidx.activity:activity | 1.8.2 |
| androidx.constraintlayout:constraintlayout | 2.1.4 |
| androidx.lifecycle:lifecycle-viewmodel-ktx / -livedata-ktx / -runtime-ktx | 2.7.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.7.3 |
| androidx.navigation:navigation-fragment-ktx / -ui-ktx | 2.7.7 |
| junit:junit (pruebas unitarias) | 4.13.2 |
| androidx.test.ext:junit / espresso-core (pruebas instrumentadas) | 1.1.5 / 3.5.1 |

Motores de datos bajo estudio: **SQLite** (incluido en Android, sin dependencia adicional) y **Firebase**
(aún no incorporado al proyecto; su versión se fijará aquí cuando se agregue).

## 3. Parámetros de configuración modificados

Archivo `gradle.properties`:

| Parámetro | Valor |
|---|---|
| `org.gradle.jvmargs` | `-Xmx2048m -Dfile.encoding=UTF-8` |
| `android.useAndroidX` | `true` |
| `android.nonTransitiveRClass` | `true` |
| `kotlin.code.style` | `official` |

Ejecución del contenedor: `--no-daemon` (sin demonio de Gradle residente) y caché de Gradle en un volumen Docker.

## 4. Equipo donde se ejecuta

| Elemento | Valor |
|---|---|
| Modelo de CPU | `[COMPLETAR]` |
| Memoria RAM | `[COMPLETAR]` |
| Sistema operativo y versión | `[COMPLETAR]` |
| Versión de Docker | `[COMPLETAR]` (`docker --version`) |
| Dispositivo o emulador para las mediciones | `[COMPLETAR]` |

## 5. Cómo ejecutar

Desde la raíz del repositorio:

```bash
docker compose up --build --abort-on-container-exit
```

Esto construye la imagen, compila el APK de depuración y ejecuta las pruebas unitarias.
El contenedor termina con código `0` si todo pasó. Los reportes quedan en `app/build/reports/tests/`.

Sin Docker (requiere JDK 17, Android SDK y Gradle 8.5):

```bash
gradle assembleDebug testDebugUnitTest
```

## 6. Alcance actual

Este entorno cubre **compilación y pruebas unitarias**. La ejecución de mediciones sobre un dispositivo o
emulador (pruebas instrumentadas) todavía no está automatizada en el contenedor.
