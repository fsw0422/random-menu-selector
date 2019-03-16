#!/bin/bash

sbt docker:publish
ssh kev@95.179.163.118 <<-EOF
	git clone https://github.com/fsw0422/random-menu-selector
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml down
	docker-compose -f ~/random-menu-selector/cicd/docker-compose.yml up -d
EOF