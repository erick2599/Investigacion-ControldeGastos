# Entorno de ejecución

El proyecto es una aplicación Android con Gradle. La forma recomendada de
compilar y probar es usar Android Studio o un JDK 17 con el SDK de Android
instalado.

## 1. Requisitos

- JDK 17 (por ejemplo, Eclipse Temurin 17).
- Android SDK con:
  - `platforms;android-34`
  - `build-tools;34.0.0`
  - `platform-tools`
- Android Studio (opcional, pero simplifica la instalación del SDK).
- `app/google-services.json` del proyecto Firebase
  `controldegastosapp-3d716` (ya está incluido en el repositorio).

Configura la ruta del SDK en `local.properties` o con la variable
`ANDROID_HOME` / `ANDROID_SDK_ROOT`:

```properties
sdk.dir=C:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
```

## 2. Comandos principales

Desde la raíz del proyecto:

```bash
# Compilar el APK de depuración
./gradlew assembleDebug

# Ejecutar las pruebas unitarias de la JVM
./gradlew test

# Ejecutar el análisis estático
./gradlew lintDebug

# Compilar la versión de release (requiere configurar la firma)
./gradlew assembleRelease
```

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

### Firma del release

`app/build.gradle.kts` lee `keystore.properties` de la raíz del proyecto (está
ignorado por Git). Si el archivo existe, `assembleRelease` firma el APK; si no
(por ejemplo en CI), el release se genera sin firmar como antes.

```properties
storeFile=controldegastos-release.jks
storePassword=...
keyAlias=controldegastos
keyPassword=...
```

El APK firmado queda en `app/build/outputs/apk/release/app-release.apk`.
Guarda una copia de `controldegastos-release.jks` y de `keystore.properties`
fuera del repositorio: sin esa keystore no se pueden publicar actualizaciones
con la misma identidad.

## 3. Pruebas instrumentadas

Las pruebas de `app/src/androidTest` requieren un emulador o un dispositivo
conectado:

```bash
./gradlew connectedDebugAndroidTest
```

## 4. Docker

El `Dockerfile` incluido es un **entorno mínimo para validar el Gradle Wrapper**
(`./gradlew --version`). No incluye el SDK de Android ni un emulador, por lo que
no compila la aplicación por sí solo:

```bash
docker build -t controldegastos .
docker run --rm controldegastos
```

Para compilar dentro de Docker habría que añadir el SDK de Android y las
licencias al Dockerfile, lo que aumenta mucho el tamaño de la imagen. Para el
flujo normal se recomienda usar Android Studio o el SDK local.

## 5. Configuración de Firebase

La inicialización de datos, las reglas de seguridad y el inicio de sesión con
Google se explican en [`SEGURIDAD-FIREBASE.md`](SEGURIDAD-FIREBASE.md).
