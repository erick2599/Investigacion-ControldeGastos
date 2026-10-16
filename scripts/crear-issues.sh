#!/usr/bin/env bash
# Crea las etiquetas y un issue por cada objetivo específico del proyecto.
# Requisitos: GitHub CLI (https://cli.github.com) instalado y autenticado (gh auth login).
# Uso, desde la raíz del repositorio:   bash scripts/crear-issues.sh
set -euo pipefail

# ---------------------------------------------------------------------------
# AJUSTA quién es responsable de cada objetivo (usuario de GitHub).
# Los valores iniciales siguen el orden de integrantes del README y son solo una propuesta.
RESP_OE1="erick2599"
RESP_OE2="JustinSanchez2541"
RESP_OE3="Stephenz16"
RESP_OE4="josecrespin071-stack"
RESP_OE5="2512652023-cpu"
# ---------------------------------------------------------------------------

command -v gh >/dev/null || { echo "Instala GitHub CLI: https://cli.github.com"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "Ejecuta primero: gh auth login"; exit 1; }

echo "Creando etiquetas..."
gh label create "objetivo-especifico" --color 0E8A16 --description "Objetivo específico de la investigación" --force
gh label create "entrega-2"           --color 5319E7 --description "Entrega 2: marco teórico y metodología"    --force
for n in 1 2 3 4 5; do
  gh label create "OE$n" --color 1D76DB --description "Objetivo específico $n" --force
done

crear() {  # $1=número  $2=título  $3=texto del objetivo  $4=responsable
  gh issue create \
    --title "OE$1 · $2" \
    --label "objetivo-especifico,OE$1,entrega-2" \
    --assignee "$4" \
    --body "**Objetivo específico $1**

$3

**Responsable:** @$4

**Criterio de cierre:** _definir en equipo_"
}

crear 1 "Diseñar la base de datos experimental común" \
  "Diseñar una base de datos experimental común para los gestores seleccionados." "$RESP_OE1"
crear 2 "Implementar el banco de pruebas automatizado" \
  "Implementar un banco de pruebas automatizado para ejecutar operaciones de consulta e inserción sobre diferentes volúmenes de datos." "$RESP_OE2"
crear 3 "Medir el tiempo de respuesta con múltiples ejecuciones" \
  "Medir el tiempo de respuesta de las operaciones evaluadas mediante múltiples ejecuciones para cada escenario experimental." "$RESP_OE3"
crear 4 "Analizar las diferencias de rendimiento" \
  "Analizar las diferencias de rendimiento obtenidas entre Firebase y SQLite." "$RESP_OE4"
crear 5 "Proponer recomendaciones de selección de gestor" \
  "Proponer recomendaciones para la selección de un gestor de bases de datos según las condiciones evaluadas y los resultados obtenidos." "$RESP_OE5"

echo "Listo. Revisa la pestaña Issues del repositorio."
