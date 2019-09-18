#!/bin/bash

sbt docker:publish

echo Enter configuration unlock passphrase:
read -s password

ssh root@fsw0422.com <<-EOF
	git clone git@github.com:fsw0422/random-menu-selector.git || git -C ~/random-menu-selector pull
	echo $password | gpg --batch --yes --passphrase-fd 0 ~/random-menu-selector/cicd/production.env.gpg
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml pull
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml down
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml up -d
EOF