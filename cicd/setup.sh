#!/usr/bin/env bash

# Build image for build environment
build-env/build.sh

# Install all Python packages
python3 -m venv venv
venv/bin/pip3 install wheel==0.34.2
venv/bin/pip3 install -r requirements.txt
