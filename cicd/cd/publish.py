from os import system, environ
from platform import system as os_type
from subprocess import call

OS_TYPE = os_type()

print("======================================")
print("Build environment detected {os_type}".format(os_type=OS_TYPE))
print("======================================")
if "Linux" in OS_TYPE:
    user_os = environ["USER"]
    user_home_os = "/home/" + user_os
else:
    user_os = "root"
    user_home_os = "/" + user_os

RUNNING_USER=""
USER_HOME=""
if "linux-gnu" in OS_TYPE:
	RUNNING_USER=${USER}
	USER_HOME="/home/"${RUNNING_USER}
else:
	RUNNING_USER="root"
	USER_HOME="/"${RUNNING_USER}
else
	echo "Build environment does not support operating systems other than Mac or Linux Distros!"
	exit 1
fi


echo ""
echo "========================================="
echo "CD environment detected [$OSTYPE]"
echo "Running container user as ${RUNNING_USER}"
echo "========================================="
echo ""

echo "Enter configuration unlock passphrase:"
read -s password
echo "$password" | gpg --batch --yes --passphrase-fd 0 "${PWD}"/production.env.gpg

echo ""
echo "==================================="
echo "Following operations are commencing"
echo "- Database evolution"
echo "- Container image publish"
echo "==================================="
echo ""
docker run -it \
	--env-file ${PWD}/production.env \
	-u ${RUNNING_USER} \
  -w ${USER_HOME}/random_menu_selector \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-v ${HOME}/.docker:${USER_HOME}/.docker \
	-v ${HOME}/.ivy2:${USER_HOME}/.ivy2 \
	-v ${HOME}/.sbt/boot:${USER_HOME}/.sbt/boot \
	-v ${PWD}/..:${USER_HOME}/random_menu_selector \
	fsw0422/random_menu_selector/cicd:latest \
	bash -c "docker login && db/evolve.sh && sbt '"';clean ;docker:publish'"'"
