#!/usr/bin/env python3

"""
We use the build image for testing, to provide the same environment as CICD pipeline
This will provide us the following advantages
1. We can actually test the build image itself if it satisfies the dependencies before pushing to actual CICD environment
2. Scripts that needs to be ran only once globally prior to the the CD process (such as database schema evolution) can be properly tested
In the end, CICD pipeline is just a naive sequential command executor
"""

from testcontainers.postgres import PostgresContainer
import os
import platform
import argparse

parser = argparse.ArgumentParser()
parser.add_argument(
    "-d",
    help="Run the test in debug mode (By default, 5005 port is opened for debug port)",
    action='store_true'
)
args, leftover = parser.parse_known_args()
if args.d:
    print("**************************************")
    print("* Debug mode is enabled in port 5005 *")
    print("**************************************")

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

os.system("docker rm -f random_menu_selector_build")
os.system("docker rm -f random_menu_selector_db")
os.system("printf 'y\n' | docker network prune")
os.system("printf 'y\n' | docker volume prune")

os.system("docker network create random_menu_selector_network")
db_container = PostgresContainer("postgres:9.6").with_name("random_menu_selector_db")
with db_container as db:
    os.system("docker network connect random_menu_selector_network random_menu_selector_db")
    os.system(
        "docker run" +
        " --name random_menu_selector_build"
        " --network=random_menu_selector_network" +
        ["", " -p 5005:5005"][args.d] +
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
        ["", " -jvm-debug 5005"][args.d] +
        " \\\";clean ;test\\\""
        "\""
    )
