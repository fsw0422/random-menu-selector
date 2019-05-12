#!/bin/bash

# TODO: make test network for testing
docker run --net host \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v ${PWD}:/workspace/random-menu-selector \
  -v $HOME/.ivy2:/root/.ivy2 \
  -e WORKSPACE=${PWD} \
  fsw0422/random-menu-selector-build:latest \
  bash -c "cd /workspace/random-menu-selector && \
		sbt -jvm-debug 5005 \
			-DPOSTGRES_PASSWORD=fake \
			-DPOSTGRES_HOST=localhost \
			-DPOSTGRES_PORT=54320 \
			-DPOSTGRES_DB=random_menu_selector \
			-DPOSTGRES_SSL_MODE=disable \
			-DWRITE_PASSWORD=fake \
			-DEMAIL_PASSWORD=fake \
			\";clean ;scapegoat ;coverage ;test ;coverageReport\""
