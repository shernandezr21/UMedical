#!/usr/bin/env bash
# Deja la base de datos y los PDF como el primer dia (datos semilla de schema.sql).
# NO toca los certificados HTTPS. Util si alguien ensucia los datos antes de la demo.
set -euo pipefail
cd "$(dirname "$0")/.."

read -r -p "Esto BORRA todos los datos (citas, usuarios nuevos, PDF). Escribe SI para continuar: " ok
[ "$ok" = "SI" ] || { echo "Cancelado."; exit 1; }

docker compose -f docker-compose.prod.yml stop api db
docker compose -f docker-compose.prod.yml rm -f api db
docker volume rm umedical_db_data umedical_uploads_data
docker compose -f docker-compose.prod.yml up -d
echo "Listo: datos de ejemplo restaurados (la API tarda ~1 minuto en volver)."
