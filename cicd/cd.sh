#!/usr/bin/env bash

echo "Enter configuration unlock passphrase:"
read -s password
echo $password | gpg --batch --yes --passphrase-fd 0 ${PWD}/cicd/production.env.gpg

echo ""
echo "====================================="
echo "Following operations are commencing"
echo "- Database evolution"
echo "- Container image publish"
echo "====================================="
echo ""
docker run -it \
	--env-file ${PWD}/cicd/production.env \
	-v /var/run/docker.sock:/var/run/docker.sock \
	-v ${HOME}/.ivy2/cache:/root/.ivy2/cache \
	-v ${HOME}/.sbt/boot:/root/.sbt/boot \
	-v ${PWD}:/random_menu_selector \
	fsw0422/random_menu_selector/cicd:latest \
	bash -c "docker login && cd /random_menu_selector && db/evolve.sh && sbt docker:publish && rm -rf target"

if [ $? -eq 0 ]
then
	echo ""
	echo "====================================="
  echo "Following operations have succeeded"
	echo "- Database evolution"
	echo "- Container image publish"
	echo "====================================="
	echo ""
	echo "====================================="
	echo "Deployment Commencing"
	echo "====================================="
	echo ""
	ssh root@fsw0422.com <<-EOF
		git clone git@github.com:fsw0422/random-menu-selector.git || git -C ~/random-menu-selector pull
		echo $password | gpg --batch --yes --passphrase-fd 0 ~/random-menu-selector/cicd/production.env.gpg

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
