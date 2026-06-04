#!/bin/bash
# Daily PostgreSQL backup for marmoraria_orcamentos.
# Usage: ./backup.sh
# Recommended cron (run as app user, daily at 2am):
#   0 2 * * * PGPASSWORD=xxx /opt/marmoraria/scripts/backup.sh >> /var/log/marmoraria-backup.log 2>&1
#
# Required env vars:
#   DB_HOST      — PostgreSQL host (default: localhost)
#   DB_PORT      — PostgreSQL port (default: 5432)
#   DB_NAME      — Database name (default: marmoraria_orcamentos)
#   DB_USER      — PostgreSQL user (default: postgres)
#   PGPASSWORD   — PostgreSQL password (must be set in environment)
#   BACKUP_DIR   — Directory to store backups (default: /var/backups/marmoraria)
#   RETAIN_DAYS  — Number of days to keep backups (default: 30)

set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-marmoraria_orcamentos}"
DB_USER="${DB_USER:-postgres}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/marmoraria}"
RETAIN_DAYS="${RETAIN_DAYS:-30}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="${DB_NAME}_${TIMESTAMP}.sql.gz"
FILEPATH="${BACKUP_DIR}/${FILENAME}"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Starting backup: ${FILENAME}"

TEMPFILE="${FILEPATH}.tmp"
pg_dump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --no-password \
  --format=plain \
  --clean \
  --if-exists \
  "${DB_NAME}" | gzip > "${TEMPFILE}" && mv "${TEMPFILE}" "${FILEPATH}"

echo "[$(date)] Backup completed: ${FILEPATH} ($(du -sh "${FILEPATH}" | cut -f1))"

# Remove backups older than RETAIN_DAYS
find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" -mtime +${RETAIN_DAYS} -delete
echo "[$(date)] Old backups pruned (kept last ${RETAIN_DAYS} days)"
