#!/usr/bin/env bash

echo "======================================"
echo "Build environment detected [$OSTYPE]"
echo "======================================"

if [[ "$OSTYPE" == *"linux-gnu"* ]]; then
	# In Linux, Docker user ids are mapped host to container one-to-one by default
	# The problem arises when container starts creating files into directories that are mounted to host,
	# causing the host to have no permission to write to the created files
	# It is better to create a user in the container that maps to the same userId / groupId to the host
	docker build \
		--build-arg USER_NAME=${USER} \
		--build-arg USER_ID=$(id -u ${USER}) \
		--build-arg GROUP_ID=$(id -g ${USER}) \
		-t fsw0422/random_menu_selector/cicd:latest \
		cicd/build-env
elif [[ "$OSTYPE" == *"darwin"* ]]; then
	# Mac users are not running Docker natively and by utilizing transparent virtual machines (HyperKit)
	# These virtual machines do not have this problem since container to host userId mapping is not the same as Linux
	# We just use root by default
	docker build \
	  -t fsw0422/random_menu_selector/cicd:latest \
		cicd/build-env
else
	echo "Build environment does not support operating systems other than Mac or Linux Distros!"
	exit 1
fi
