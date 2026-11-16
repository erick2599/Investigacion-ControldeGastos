# Investigacion-ControldeGastos
** Erick Vlasdimir Gaitan Ramirez 25-3677-2020
Usuario de Github https://github.com/erick2599 **

** Justin Brian Sanchez Cardona  25-4195-2024
Usuario de Github https://github.com/JustinSanchez2541 **

** Wilson Stephen Martínez Alfaro 25-2887-2024
Usuario de Github https://github.com/Stephenz16 **

** Jose Ernesto Crespin Sayes  25-0599-2024
Usuario de Github https://github.com/josecrespin071-stack **

** David Reynaldo Trejo Jimenez  25-1265-2023
Usuario de Github https://github.com/2512652023-cpu **

** Diego Anthony Ramos Alemán 25-3224-2021
Usuario de Github https://github.com/Gowi55 **


OBJETIVOS

Objetivo general
Comparar el rendimiento de Firebase,  y SQLite en operaciones de consulta e inserción utilizando diferentes volúmenes de datos bajo condiciones controladas.


Objetivos específicos
1.	Diseñar una base de datos experimental común para los tres gestores seleccionados.
2.	Implementar un banco de pruebas automatizado para ejecutar operaciones de consulta e inserción sobre diferentes volúmenes de datos.
3.	Medir el tiempo de respuesta de las operaciones evaluadas mediante múltiples ejecuciones para cada escenario experimental.
4.	Analizar las diferencias de rendimiento obtenidas entre Firebase y SQLite.
5.	Proponer recomendaciones para la selección de un gestor de bases de datos según las condiciones evaluadas y los resultados obtenidos.


## Estado de la aplicación

La app implementa el gestor de gastos sobre **Firebase Authentication,
Firestore y Storage**:

- Registro e inicio de sesión con correo/contraseña y recuperación de contraseña.
- Inicio de sesión con Google (opcional; requiere configurar un ID de cliente
  web, ver la documentación).
- Registro de gastos e ingresos con validación de campos.
- Balance calculado en centavos para evitar errores de redondeo.
- Recibos fotográficos subidos a Firebase Storage.
- Datos aislados por usuario mediante reglas de Firestore y Storage.

## Cómo compilar y probar

Requisitos: JDK 17 y Android SDK 34. Los detalles están en
[`Docs/entorno.md`](Docs/entorno.md).

```bash
./gradlew assembleDebug   # APK de depuración
./gradlew test            # pruebas unitarias
./gradlew lintDebug       # análisis estático
./gradlew assembleRelease # release con R8
```

Antes de usar la app con datos reales, despliega las reglas de seguridad:
[`Docs/SEGURIDAD-FIREBASE.md`](Docs/SEGURIDAD-FIREBASE.md).

## Estructura del código

```
app/src/main/java/com/example/controldegastos/
├── data/       # GastosRepository + implementación Firebase
├── domain/     # Dinero, Fechas, Validador y BalanceCalculator (sin Android)
├── model/      # Gasto, Ingreso, Usuario
├── view/       # Activities y adaptador
└── viewModel/  # GastosViewModel y estado de la UI
```

## Pruebas

Las pruebas unitarias cubren el cálculo monetario, las validaciones, el manejo
de fechas y el balance del ViewModel con un repositorio falso:

```bash
./gradlew test
```

Las pruebas instrumentadas requieren un emulador o dispositivo:

```bash
./gradlew connectedDebugAndroidTest
```

Las reglas de Firestore y Storage tienen su propia suite contra los emuladores de
Firebase (22 casos: aislamiento entre usuarios, ruta heredada por email,
validación de campos y límites de los recibos). Requiere Node 22+ y Java 21+:

```bash
cd firebase-tests
npm ci
npm run test:rules
```

El proyecto `demo-controldegastos` es ficticio: los emuladores no tocan el
proyecto real. Instrucciones completas en
[`Docs/SEGURIDAD-FIREBASE.md`](Docs/SEGURIDAD-FIREBASE.md).


