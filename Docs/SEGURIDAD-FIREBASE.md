# Seguridad y configuración de Firebase

Este documento resume los pasos manuales que **no** se pueden completar desde el
código porque dependen de la consola de Firebase / Google Cloud.

## 1. Reglas de Firestore y Storage

Los archivos ya están en la raíz del repositorio:

- `firestore.rules`: cada usuario solo accede a `usuarios/{suid}` y a sus
  subcolecciones `gastos` e `ingresos`. También se permite temporalmente la ruta
  antigua `usuarios/{email}` para poder migrar los datos existentes.
- `storage.rules`: los recibos viven en `usuarios/{uid}/recibos/{archivo}` y solo
  los puede leer o escribir su propietario (imágenes de máximo 5 MB).
- `firebase.json` y `firestore.indexes.json`: configuración para la CLI.

Para publicarlas (requiere `npm i -g firebase-tools` y haber iniciado sesión):

```bash
firebase login
firebase use controldegastosapp-3d716
firebase deploy --only firestore:rules,storage
```

### 1.1. Probarlas antes de desplegar

Las reglas se validan con los emuladores de Firebase, sin tocar el proyecto real.
La suite está en `firebase-tests/` y cubre aislamiento entre usuarios, la ruta
heredada por email, campos obligatorios, montos fuera de rango, tipos de archivo
y el límite de 5 MB de los recibos.

Requisitos: Node 22+ y Java 21+ (el CLI de Firebase ya no admite versiones
anteriores de Java).

```bash
cd firebase-tests
npm ci
npm run test:rules
```

El resultado esperado es `22 passed`. El script arranca los emuladores en el
proyecto ficticio `demo-controldegastos`, ejecuta Vitest y los apaga al terminar.
También se ejecuta en el trabajo `firebase-rules` de GitHub Actions.

> Si el proyecto sigue en **modo de prueba**, cualquier persona con la API key
> podría leer o escribir datos. Desplegar las reglas es el paso más importante.
> Estado actual: las reglas de Firestore están publicadas y una lectura sin
> autenticación devuelve `403 PERMISSION_DENIED`. Las de Storage quedan
> pendientes hasta activar el plan Blaze.

### 1.2. Storage requiere Blaze

Cloud Storage for Firebase ya no está disponible en el plan Spark: la consola
pide un método de pago para activar **Blaze**. No es un cobro si se respeta el
nivel sin costo, pero solo aplica si el bucket está en `us-central1`,
`us-west1` o `us-east1`:

| Concepto | Sin costo al mes |
|---|---|
| Almacenado | 5 GB |
| Descargado | 100 GB |
| Subidas | 5.000 |
| Descargas | 50.000 |

Con el límite de 5 MB por recibo, 5 GB equivalen a ~1.000 fotos. Conviene crear
una alerta de presupuesto en Google Cloud (Facturación → Presupuestos); la
alerta avisa, no bloquea el gasto.

Después de comprobar que todos los usuarios migraron a `usuarios/{uid}`, se puede
eliminar la condición del email en `firestore.rules` y volver a desplegar.

## 2. Migración de datos antiguos

La versión anterior guardaba los datos en `usuarios/{email}`. Al iniciar sesión,
`FirebaseGastosRepository.migrarDatosLegacy()`:

1. Copia `gastos` e `ingresos` a `usuarios/{uid}` por lotes de 400 documentos.
2. Combina el perfil con `SetOptions.merge()`.
3. Elimina la ruta antigua solo si la copia terminó correctamente.

Es idempotente: si falla a mitad de camino, se reintenta en el siguiente inicio
de sesión. Conviene probarlo primero con una cuenta de prueba en un proyecto
Firebase de desarrollo.

## 3. Inicio de sesión con Google

El botón se **oculta** si no hay un ID de cliente web configurado. En este
proyecto ya está configurado:

- Web client ID en `app/src/main/res/values/strings.xml`.
- SHA-1 de depuración:
  `3E:77:FA:6A:92:25:17:54:1D:ED:AC:94:1E:7C:34:BD:E2:E3:D2:92`.
- El proveedor Google está habilitado (verificado contra Identity Toolkit).

Falta probar el flujo en un dispositivo y, si se firma el release, registrar
también su SHA-1. Para replicar la configuración en otro entorno:

1. En Google Cloud Console → *APIs y servicios* → *Credenciales*, crea un
   **ID de cliente de OAuth 2.0** de tipo *Aplicación web*.
2. Registra la huella SHA-1 de la keystore de depuración y de la de release en
   la configuración de la app Android de Firebase.
3. Vuelve a descargar `app/google-services.json`; debe incluir un bloque
   `oauth_client` con el cliente web.
4. Copia el ID (termina en `.apps.googleusercontent.com`) en
   `app/src/main/res/values/strings.xml`:

```xml
<string name="google_web_client_id" translatable="false">1234567890-xxxx.apps.googleusercontent.com</string>
```

5. Vuelve a compilar. Si el ID es inválido o falta el SHA-1, Google Sign-In
   seguirá fallando.

> La API `GoogleSignIn` de Google Play Services está marcada como obsoleta. La
> migración recomendada es **Credential Manager**; se dejó la implementación
> actual para no romper el flujo existente, pero es una deuda técnica pendiente.

## 4. Protección de la API key

La API key de `google-services.json` no es un secreto por sí sola, pero conviene
restringirla en Google Cloud Console:

- *Restricciones de aplicación*: solo la app Android
  `com.example.controldegastos` con el SHA-1 registrado.
- *Restricciones de API*: solo las APIs que usa el proyecto (Identity Toolkit,
  Cloud Firestore, Firebase Storage, etc.).

## 5. Copias de seguridad

`AndroidManifest.xml` referencia `res/xml/backup_rules.xml` y
`res/xml/data_extraction_rules.xml`. Los tokens de autenticación de Firebase se
excluyen de la copia en la nube y de la transferencia entre dispositivos.

## 6. Actualización pendiente de SDK

El proyecto compila con `compileSdk = 34` y `targetSdk = 34`. Google Play exige
`targetSdk = 36` para las aplicaciones nuevas a partir de 2026. La actualización
no se aplicó en este cambio porque el entorno solo tiene instalados
`android-34` y `android-36.1`, y la combinación actual AGP 8.2.2 / Gradle 8.7 no
es compatible con `compileSdk 36`.

Ruta recomendada:

1. Instalar Android Studio Ladybug o superior.
2. Actualizar Gradle a 8.9+ y AGP a 8.7+ (o AGP 8.9).
3. Instalar `platforms;android-36` y `build-tools;36.0.0`.
4. Cambiar `compileSdk` y `targetSdk` a 36.
5. Ejecutar `gradlew lintDebug test assembleDebug` y probar en un dispositivo.
6. Revisar los cambios de comportamiento de Android 15/16 (edge-to-edge,
   permisos, foreground services).
