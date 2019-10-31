#!/usr/bin/env python3

from testcontainers.postgres import PostgresContainer
import os
import platform

OS_TYPE = platform.system()

USER = ""
USER_HOME = ""
if OS_TYPE == "Linux":
    USER = os.environ["USER"]
    USER_HOME = "/home/" + USER
elif OS_TYPE == "Darwin":
    USER = "root"
    USER_HOME = "/" + USER
else:
    print("Unsupported OS")
    exit(1)

print("=========================================")
print("CI environment detected [" + OS_TYPE + "]")
print("Running container user as " + USER)
print("Test commencing")
print("=========================================")

os.system("printf 'y\n' | docker volume prune")
db_container = PostgresContainer("postgres:9.6")
with db_container as db:
    os.system(
        "docker run" +
        " --network=host" +
        " -u " + USER +
        " -w " + USER_HOME +
        " -v /var/run/docker.sock:/var/run/docker.sock" +
        " -v $HOME/.ivy2:" + USER_HOME + "/.ivy2" +
        " -v $HOME/.sbt:" + USER_HOME + "/.sbt" +
        " -v $PWD:" + USER_HOME + "/random_menu_selector" +
        " -e POSTGRES_HOST=" + db.get_container_host_ip() +
        " -e POSTGRES_PORT=" + db.get_exposed_port(5432) +
        " -e POSTGRES_SSL_MODE=" + os.environ["POSTGRES_SSL_MODE"] +
        " -e POSTGRES_DB=" + os.environ["POSTGRES_DB"] +
        " -e POSTGRES_USER=" + os.environ["POSTGRES_USER"] +
        " -e POSTGRES_PASSWORD=" + os.environ["POSTGRES_PASSWORD"] +
        " -e WRITE_PASSWORD=" + os.environ["WRITE_PASSWORD"] +
        " -e EMAIL_USER=" + os.environ["EMAIL_USER"] +
        " -e EMAIL_PASSWORD=" + os.environ["EMAIL_PASSWORD"] +
        " fsw0422/random_menu_selector/cicd:latest" +
        " bash -c \"cd random_menu_selector && db/evolve.sh && sbt test\""
    )
