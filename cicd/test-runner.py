#!/usr/bin/env python3

from testcontainers.postgres import PostgresContainer
import os
import platform

OS_TYPE = platform.system()

USER = ""
USER_HOME = ""
if "Linux" in OS_TYPE:
    USER = os.environ["USER"]
    USER_HOME = "/home/" + USER
elif "Darwin" in OS_TYPE:
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
        " --network=host"
        " -u " + USER +
        " -w " + USER_HOME +
        " -v /var/run/docker.sock:/var/run/docker.sock" +
        " -v ${HOME}/.ivy2:" + USER_HOME + "/.ivy2" +
        " -v ${HOME}/.sbt/boot:" + USER_HOME + "/.sbt/boot" +
        " -v ${PWD}:" + USER_HOME + "/random_menu_selector" +
        " -e POSTGRES_HOST=" + os.environ["POSTGRES_HOST"] +
        " -e POSTGRES_PORT=" + db.get_exposed_port(os.environ["POSTGRES_PORT"]) + # Map port to the randomized port
        " -e POSTGRES_SSL_MODE=" + os.environ["POSTGRES_SSL_MODE"] +
        " -e POSTGRES_DB=" + os.environ["POSTGRES_DB"] +
        " -e POSTGRES_USER=" + os.environ["POSTGRES_USER"] +
        " -e POSTGRES_PASSWORD=" + os.environ["POSTGRES_PASSWORD"] +
        " -e WRITE_PASSWORD=" + os.environ["WRITE_PASSWORD"] +
        " -e EMAIL_USER=" + os.environ["EMAIL_USER"] +
        " -e EMAIL_PASSWORD=" + os.environ["EMAIL_PASSWORD"] +
        " fsw0422/random_menu_selector/cicd:latest" +
        " bash -c \"cd " + USER_HOME + "/random_menu_selector && db/evolve.sh && sbt \\\";clean ;test\\\"\""
    )
