# ADR 0001: Elección de tecnologías de almacenamiento para el benchmark

## Estado
Propuesta — pendiente de aprobación del equipo.

## Contexto
El prototipo Android de gestor de gastos personales requiere guardar y 
consultar registros de gastos, y a la vez comparar el rendimiento de 
almacenamiento local frente a almacenamiento en la nube. El repositorio 
base está construido con Gradle/Kotlin y define como objetivo comparar 
Firebase y SQLite en operaciones de inserción y consulta con volúmenes 
de 100, 1.000 y 10.000 registros.

## Opciones consideradas
- **(A) SQLite local**: motor relacional embebido, sin dependencia de red.
- **(B) Firebase Realtime Database**: base de datos en la nube basada en árbol JSON.
- **(C) Cloud Firestore**: base de datos documental en la nube.
- **(D) Combinación SQLite + sincronización cloud**.

## Decisión
Para el experimento se comparará **SQLite local** contra **un solo 
producto de Firebase**, el cual debe declararse y fijarse antes de 
iniciar las mediciones, para evitar mezclar resultados de productos 
distintos (Realtime Database vs Firestore) como si fueran una sola 
tecnología.

La elección final entre Realtime Database y Cloud Firestore debe 
confirmarse revisando el código y la configuración actual del 
repositorio, y registrarse en `docs/entorno.md`.

## Justificación
- Se necesita una comparación válida entre almacenamiento local y cloud.
- Mezclar Realtime Database y Firestore invalidaría la comparación.
- Fijar versiones y configuración permite reproducibilidad.
- ISO/IEC 25010:2023 (eficiencia de desempeño) respalda medir tiempo 
  de respuesta como variable principal.

## Consecuencias
**Positivas:**
- Se obtendrán tiempos comparables bajo condiciones definidas.
- El protocolo queda replicable por terceros.

**Negativas / Riesgos:**
- El resultado no representa por sí solo costo, seguridad, escalabilidad 
  ni experiencia total del usuario.
- Las mediciones de Firebase incluyen dependencia de red y caché, que 
  deben documentarse explícitamente.
- Se deben proteger credenciales y reglas de acceso en el entorno de prueba.

## Referencias
- ISO/IEC 25010:2023 — Product quality model.
- Kabakuş (2019) — Comparación SQLite vs Firebase.
- Kot & Smołka (2023) — Análisis de rendimiento cloud en móviles.
- Documentación oficial de Firebase y SQLite.
