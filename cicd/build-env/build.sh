#!/usr/bin/env bash

docker build \
		-t fsw0422/random_menu_selector/cicd:latest \
		cicd/build-env
