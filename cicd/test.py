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
print("Test is commencing")
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
        " -e POSTGRES_USER=" + db.POSTGRES_USER +
        " -e POSTGRES_PASSWORD=" + db.POSTGRES_PASSWORD +
        " -e POSTGRES_DB=" + db.POSTGRES_DB +
        " -e POSTGRES_HOST=" + db.get_container_host_ip() +
        " -e POSTGRES_PORT=" + db.get_exposed_port(5432) +
        " -e POSTGRES_SSL_MODE=disable" +
        " -e WRITE_PASSWORD=fake" +
        " -e EMAIL_USER=fake@mail.com" +
        " -e EMAIL_PASSWORD=fake" +
        " fsw0422/random_menu_selector/cicd:latest" +
        " bash -c \"cd random_menu_selector && db/evolve.sh && sbt -jvm-debug 5005 ';clean ;scapegoat ;coverage ;test ;coverageReport'\""
    )
