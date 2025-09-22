#!/bin/bash

# Carrega as variáveis do arquivo .env
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
else
    echo "[terraform] Erro: Arquivo .env não encontrado."
    exit 1
fi

# Verifica se o método foi passado como argumento
if [ -z "$1" ]; then
    echo "[terraform] Erro: Nenhum método especificado (plan, apply, etc.)."
    exit 1
fi

METHOD=$1
shift

PARAMS="$@"

terraform $METHOD $PARAMS \
-var "aws_region=$AWS_REGION" \
-var "aws_s3_bucket_name=$AWS_S3_BUCKET_NAME" \
-var "aws_s3_access_key_id=$AWS_S3_ACCESS_KEY_ID" \
-var "aws_s3_secret_access_key=$AWS_S3_SECRET_ACCESS_KEY" \
-var "db_hacka_username=$DB_HACKA_USERNAME" \
-var "db_hacka_password=$DB_HACKA_PASSWORD" \
-var "db_hacka_name=$DB_HACKA_NAME" \
-var "db_hacka_port=$DB_HACKA_PORT" \
-var "dockerhub_username=$DOCKERHUB_USERNAME"