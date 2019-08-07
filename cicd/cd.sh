#!/bin/bash

sbt docker:publish
ssh kev@$fsw0422.com <<-EOF
	git clone https://github.com/fsw0422/random-menu-selector || git -C ~/random-menu-selector pull
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml pull
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml down
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml up -d
EOF