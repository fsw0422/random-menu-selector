from subprocess import call

print("")
print("=============================")
print("Database evolution commencing")
print("=============================")
print("")


liquibase \
  --classpath=${HOME}/.ivy2/cache/org.postgresql/postgresql/bundles/postgresql-42.2.5.jar \
  --driver=org.postgresql.Driver \
  --changeLogFile=db/changelog.xml \
  --url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB} \
  --username=${POSTGRES_USER} \
  --password=${POSTGRES_PASSWORD} \
  update
