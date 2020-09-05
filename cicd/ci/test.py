from argparse import ArgumentParser
from dotenv import load_dotenv
from logging import basicConfig, info
from os import system, environ
from pathlib import Path
from platform import system as os_type
from testcontainers.postgres import PostgresContainer


def test(debug, sbt_cmd, user, user_home):
    print("=========================================")
    print("CI environment detected [" + OS_TYPE + "]")
    print("Running container user as " + user)
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
            system("mkdir -p ${HOME}/.sbt && mkdir -p ${HOME}/.ivy2")
            test = " && ".join([
                "sbt compile",  # Liquibase requires db jar dependency
                "python schema.py",
                "sbt " + ["", "-jvm-debug 5005"][debug] + " \\\"" + sbt_cmd + "\\\"",
            ])
            system(" ".join([
                "docker run",
                "--name", "random_menu_selector_build",
                "--network", "random_menu_selector_network",
                "--env-file", "${PWD}/test.env",
                ["", "-p 5005:5005"][debug],
                "-u", user,
                "-w", user_home + "/random_menu_selector",
                "-v", "/var/run/docker.sock:/var/run/docker.sock",
                "-v", "${HOME}/.ivy2:" + user_home + "/.ivy2",
                "-v", "${HOME}/.sbt:" + user_home + "/.sbt",
                "-v", "${PWD}/..:" + user_home + "/random_menu_selector",
                "fsw0422/random_menu_selector/cicd:latest",
                "bash -c \"{cmd}\"".format(cmd=test),
            ]))
    finally:
        system("docker rm -f random_menu_selector_build")
        system("docker rm -f random_menu_selector_db")
        system("printf 'y\n' | docker network prune")
        system("printf 'y\n' | docker volume prune")


if __name__ == "__main__":
    load_dotenv(dotenv_path=Path("") / "test.env")

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
    user_os = ""
    user_home_os = ""
    if "Linux" in OS_TYPE:
        user_os = environ["USER"]
        user_home_os = "/home/" + user_os
    else:
        user_os = "root"
        user_home_os = "/" + user_os

    test(args.d, args.c, user_os, user_home_os)
