#!/usr/bin/env bash
# Respaldo de la base de datos y de los PDF. Se ejecuta en el servidor, dentro del repositorio:  bash deploy/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
STAMP="$(date +%F-%H%M)"

docker compose -f docker-compose.prod.yml exec -T db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction u_medical' > "backups/umedical-$STAMP.sql"
docker run --rm -v umedical_uploads_data:/data -v "$PWD/backups":/backup alpine tar czf "/backup/pdf-$STAMP.tgz" -C /data .

echo "Respaldos en ./backups:"
ls -lh backups | tail -n 5
