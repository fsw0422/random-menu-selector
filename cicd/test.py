from argparse import ArgumentParser
from dotenv import load_dotenv
from os import system, environ
from pathlib import Path
from platform import system as os_type
from testcontainers.postgres import PostgresContainer

load_dotenv(dotenv_path=Path(".") / "cicd/test.env")

parser = ArgumentParser()
parser.add_argument(
    "-d",
    help="Run the test in debug mode (By default, 5005 port is opened for debug port)",
    action='store_true',
)
parser.add_argument(
    "-c",
    help="SBT command argument to pass in",
)
args, leftover = parser.parse_known_args()
if args.d:
    print("**************************************")
    print("* Debug mode is enabled in port 5005 *")
    print("**************************************")

OS_TYPE = os_type()

USER = ""
USER_HOME = ""
if "Linux" in OS_TYPE:
    USER = environ["USER"]
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

try:
    postgres = PostgresContainer("postgres:9.6")
    postgres.POSTGRES_DB = environ.get("POSTGRES_DB")
    postgres.POSTGRES_USER = environ.get("POSTGRES_USER")
    postgres.POSTGRES_PASSWORD = environ.get("POSTGRES_PASSWORD")
    with postgres.with_name("random_menu_selector_db") as db:
        system("docker network create random_menu_selector_network")
        system("docker network connect random_menu_selector_network random_menu_selector_db")
        system("mkdir -p {home}/.sbt && mkdir -p {home}/.ivy2".format(home=USER_HOME))
        test = " && ".join([
            "sbt compile",  # Liquibase requires db jar dependency
            "db/evolve.sh",
            "sbt " + ["", "-jvm-debug 5005"][args.d] + " \\\"" + args.c + "\\\"",
        ])
        system(" ".join([
            "docker run",
            "--name", "random_menu_selector_build",
            "--network", "random_menu_selector_network",
            "--env-file", "${PWD}/cicd/test.env",
            ["", "-p 5005:5005"][args.d],
            "-u", USER,
            "-w", USER_HOME + "/random_menu_selector",
            "-v", "/var/run/docker.sock:/var/run/docker.sock",
            "-v", "${HOME}/.ivy2:" + USER_HOME + "/.ivy2",
            "-v", "${HOME}/.sbt:" + USER_HOME + "/.sbt",
            "-v", "${PWD}:" + USER_HOME + "/random_menu_selector",
            "fsw0422/random_menu_selector/cicd:latest",
            "bash -c \"{cmd}\"".format(cmd=test),
        ]))
finally:
    system("docker rm -f random_menu_selector_build")
    system("docker rm -f random_menu_selector_db")
    system("printf 'y\n' | docker network prune")
    system("printf 'y\n' | docker volume prune")
