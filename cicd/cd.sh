#!/usr/bin/env bash

RUNNING_USER=""
USER_HOME=""
if [[ "$OSTYPE" == *"linux-gnu"* ]]; then
	RUNNING_USER=${USER}
	USER_HOME="/home/"${RUNNING_USER}
elif [[ "$OSTYPE" == *"darwin"* ]]; then
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

echo ""
echo "============================="
echo "Database evolution commencing"
echo "============================="
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

if [ $? -eq 0 ]; then
	echo ""
	echo "===================================="
  echo "Following operations have succeeded"
	echo "- Database evolution"
	echo "- Container image publish"
	echo "Deployment Commencing"
	echo "===================================="
	echo ""
	ssh kev@fsw0422.com <<-EOF
		git clone git@github.com:fsw0422/random-menu-selector.git || git -C ~/random-menu-selector pull

		cd random-menu-selector/cicd || exit
		echo $password | gpg --batch --yes --passphrase-fd 0 production.env.gpg
		docker-compose -f docker-compose.yml pull
		docker-compose -f docker-compose.yml down
		docker-compose -f docker-compose.yml up -d
	EOF

	echo ""
	echo "===================="
	echo "Deployment Succeeded"
	echo "===================="
	echo ""
	exit 0
else
	echo ""
	echo "================="
	echo "Deployment Failed"
	echo "================="
	echo ""
	exit 1
fi
