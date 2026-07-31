#!/bin/bash

set -e

# Build JDBC URL if the caller did not provide a full one
if [ -z "${DB_URL}" ]; then
  export DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Nairobi&characterEncoding=UTF-8"
fi


echo "  Kimwanyi SACCO – container starting"
echo "  DB_URL  : ${DB_URL}"
echo "  DB_USER : ${DB_USERNAME}"


# Wait for MySQL
MAX_TRIES=30
TRIES=0
echo "Waiting for MySQL at ${DB_HOST}:${DB_PORT} ..."

until mysqladmin ping -h "${DB_HOST}" -P "${DB_PORT}" \
        -u "${DB_USERNAME}" --password="${DB_PASSWORD}" \
        --silent 2>/dev/null; do
  TRIES=$((TRIES + 1))
  if [ "${TRIES}" -ge "${MAX_TRIES}" ]; then
    echo "ERROR: MySQL did not become ready after ${MAX_TRIES} attempts. Aborting."
    exit 1
  fi
  echo "  MySQL not ready yet (attempt ${TRIES}/${MAX_TRIES}) – retrying in 3 s..."
  sleep 3
done

echo "MySQL is ready. Starting Tomcat..."

# Hand off to Tomcat
exec catalina.sh run
