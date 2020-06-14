#!/usr/bin/env bash

echo ""
echo "=============================="
echo "Environment detected [$OSTYPE]"
echo "=============================="
echo ""

# Install all native binary that `test-containers` depend on
if [[ "$OSTYPE" == *"linux-gnu"* ]]; then
	# Add here per distro installation script that use package manager other than `apt` if required
	sudo apt install libpq-dev python3-dev
elif [[ "$OSTYPE" == *"darwin"* ]]; then
	xcode-select --install
	brew install postgresql
else
	echo "Build environment does not support operating systems other than Mac or Debian based Linux Distros!"
	exit 1
fi

# Build image for build environment
cicd/build-env/build.sh

# Install all Python packages
python3 -m venv cicd/venv
cicd/venv/bin/pip3 install -r cicd/requirements.txt
