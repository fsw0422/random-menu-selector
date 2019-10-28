#!/usr/bin/env bash

RUNNING_USER=""
if [[ "$OSTYPE" == *"linux-gnu"* ]]; then
  RUNNING_USER=${USER}
elif [[ "$OSTYPE" == *"darwin"* ]]; then
  RUNNING_USER="root"
else
	echo "Build environment does not support operating systems other than Mac or Linux Distros!"
	exit 1
fi

echo ""
echo "====================================="
echo "CD environment detected [$OSTYPE]"
echo "Running container user as ${RUNNING_USER}"
echo "Container image publish commencing"
echo "====================================="
echo ""

docker run -it \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v ${HOME}/.ivy2/cache:/${RUNNING_USER}/.ivy2/cache \
    -v ${HOME}/.sbt/boot:/${RUNNING_USER}/.sbt/boot \
    -v ${PWD}:/random_menu_selector \
    fsw0422/random_menu_selector/cicd:latest \
    bash -c "docker login && cd /random_menu_selector && sbt docker:publish"

if [ $? -eq 0 ]
then
  echo ""
  echo "====================================="
	echo "Container image publish has succeeded"
	echo "Deployment Commencing"
  echo "====================================="
  echo ""

  echo "Enter configuration unlock passphrase:"
  read -s password
	ssh root@fsw0422.com <<-EOF
		git clone git@github.com:fsw0422/random-menu-selector.git || git -C ~/random-menu-selector pull
		echo $password | gpg --batch --yes --passphrase-fd 0 ~/random-menu-selector/cicd/production.env.gpg

    bash ~/random-menu-selector/db/evolve.sh

		docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml pull
		docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml down
		docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml up -d
	EOF

  echo ""
  echo "===================="
	echo "Deployment Succeeded"
  echo "===================="
  echo ""
	exit 0
else
  echo ""
  echo "===================="
	echo "Deployment Failed"
  echo "===================="
  echo ""
	exit 1
fi
