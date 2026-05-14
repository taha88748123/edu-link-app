#!/bin/bash
# postgres-init.sh — create one DB per microservice on first init.
# Mirrored from infrastructure/postgres-init.sh; shipped as ConfigMap to the
# Bitnami postgresql chart via primary.initdb.scriptsConfigMap.
set -euo pipefail

DATABASES=(auth_db profile_db calendar_db pricing_db booking_db payment_db video_db ai_db local_db notification_db admin_db)

for DB in "${DATABASES[@]}"; do
  echo "Creating database: $DB"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE $DB;
    GRANT ALL PRIVILEGES ON DATABASE $DB TO $POSTGRES_USER;
EOSQL
done

echo "All databases provisioned."
