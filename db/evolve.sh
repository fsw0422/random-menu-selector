#!/usr/bin/env bash

liquibase \
  --classpath=db/postgresql-42.1.4.jar \
  --driver=org.postgresql.Driver \
  --changeLogFile=db/changelog.xml \
  --url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB} \
  --username=${POSTGRES_USER} \
  --password=${POSTGRES_PASSWORD} \
  update
