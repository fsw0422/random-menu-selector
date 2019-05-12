#!/bin/bash

sbt docker:publish
echo "enter hostname:"
read host
ssh kev@${host} <<-EOF
	git clone https://github.com/fsw0422/random-menu-selector || git -C ~/random-menu-selector pull
	docker-compose -f ~/random-menu-selector/container.yml pull
	docker-compose -f ~/random-menu-selector/container.yml down
	docker-compose -f ~/random-menu-selector/container.yml up -d
EOF