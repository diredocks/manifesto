#!/bin/sh
set -e

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
until nc -z postgres 5432; do
  sleep 1
done
echo "PostgreSQL is ready"

# Wait for RabbitMQ
echo "Waiting for RabbitMQ..."
until nc -z rabbitmq 5672; do
  sleep 1
done
echo "RabbitMQ is ready"

exec java -jar app.jar
