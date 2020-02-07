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

os.system("printf 'y\n' | docker network prune")
os.system("printf 'y\n' | docker volume prune")
os.system("docker network create random_menu_selector_network")

db_container = PostgresContainer("postgres:9.6").with_name("random_menu_selector_db")
with db_container as db:
    os.system("docker network connect random_menu_selector_network random_menu_selector_db")
    os.system(
        "docker run" +
        " --network=random_menu_selector_network" +
        # TODO: make it optionable
        ["", " -p 5005:5005"][True] +
        " -u " + USER +
        " -w " + USER_HOME + "/random_menu_selector"
        " -v /var/run/docker.sock:/var/run/docker.sock" +
        " -v ${HOME}/.ivy2:" + USER_HOME + "/.ivy2" +
        " -v ${HOME}/.sbt/boot:" + USER_HOME + "/.sbt/boot" +
        " -v ${PWD}:" + USER_HOME + "/random_menu_selector" +
        " -e POSTGRES_HOST=random_menu_selector_db"
        " -e POSTGRES_PORT=5432"
        " -e POSTGRES_SSL_MODE=disable"
        " -e POSTGRES_DB=" + os.environ["POSTGRES_DB"] +
        " -e POSTGRES_USER=" + os.environ["POSTGRES_USER"] +
        " -e POSTGRES_PASSWORD=" + os.environ["POSTGRES_PASSWORD"] +
        " -e WRITE_PASSWORD=fake"
        " -e EMAIL_USER=fake@mail.com"
        " -e EMAIL_PASSWORD=fake"
        " fsw0422/random_menu_selector/cicd:latest" +
        " bash -c \""
        " db/evolve.sh &&"
        " sbt" +
        # TODO: make it optionable
        ["", " -jvm-debug 5005"][True] +
        " \\\";clean ;test\\\""
        "\""
    )
