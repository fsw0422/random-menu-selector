from docker import from_env
from docker.errors import BuildError
from platform import system as os_type
from logging import basicConfig, info, error, INFO
from sys import stdout


def build_with_logs(client, buildargs, path, tag):
    try:
        img, logs = client.build(
            buildargs=buildargs,
            path=path,
            tag=tag
        )
        for line in logs:
            if line.get("stream"):
                info(line["stream"].strip())
            elif line.get("aux"):
                info(line["aux"]["ID"].strip())
    except BuildError as e:
        for line in e.build_log:
            if line.get("stream"):
                error(line["stream"].strip())
        raise e


if __name__ == "__main__":
    basicConfig(stream=stdout, level=INFO)

    os = os_type()

    info("=========================================")
    info("Operating System Detected [{os}]".format(os=os))
    info("=========================================")

    if "Linux" in os:
        # In Linux, Docker user IDs are mapped host to container one-to-one by default
        # The problem arises when container starts creating files into directories that are mounted to host
        # This cause the host to have no permission to have write access to the created files
        # It is better to create a user in the container that maps to the same user Id / group Id to the host
        from os import environ, getgid, getuid

        build_with_logs(
            client=from_env().images,
            buildargs={
                "USER_NAME": environ["USER"],
                "USER_ID": getuid(),
                "GROUP_ID": getgid(),
            },
            path=".",
            tag="fsw0422/random_menu_selector/cicd:latest"
        )
    else:
        # Mac / Windows users are not running Docker natively and by utilizing transparent virtual machines (HyperKit, Hyper-V)
        # These virtual machines do not have this problem since container to host userId mapping is not the same as Linux
        # Also since some user IDs used in Mac is mapped to reserved user ID in Linux, we just use root by default
        build_with_logs(
            client=from_env().images,
            buildargs=None,
            path=".",
            tag="fsw0422/random_menu_selector/cicd:latest"
        )
