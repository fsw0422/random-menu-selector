#!/usr/bin/env bash

# Install all native binary that `test-containers` depend on
if [[ "$OSTYPE" == *"linux-gnu"* ]]; then
	# Add here per distro installation script that use package manager other than `apt` if required
	sudo apt install libpq-dev python3-dev
elif [[ "$OSTYPE" == *"darwin"* ]]; then
	xcode-select --install
	brew install postgresql
else
	echo "Build environment does not support operating systems other than Mac or Linux Distros!"
	exit 1
fi

# Install all Python packages
python3 -m venv ./venv
source venv/bin/activate
pip3 install -r cicd/requirements.txt
deactivate
