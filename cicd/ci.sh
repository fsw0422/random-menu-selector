#!/usr/bin/env bash

export POSTGRES_DB=random_menu_selector # Always sync this value with PROD value due to integration test (see production.env.gpg)
export POSTGRES_USER=postgres # Always sync this value with PROD value due to integration test (see production.env.gpg)
export POSTGRES_PASSWORD=fake

source venv/bin/activate
cicd/test-runner.py "$1"
deactivate
