#!/usr/bin/env bash

# Build image for build environment
cicd/build-env/build.sh

# Install all Python packages
python3 -m venv cicd/venv
cicd/venv/bin/pip3 install wheel==0.34.2
cicd/venv/bin/pip3 install -r cicd/requirements.txt
