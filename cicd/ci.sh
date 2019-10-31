#!/usr/bin/env bash

export POSTGRES_DB=random_menu_selector # Always sync this value with PROD value due to integration test (see production.env.gpg)
export POSTGRES_USER=postgres # Always sync this value with PROD value due to integration test (see production.env.gpg)
export POSTGRES_PASSWORD=fake
export POSTGRES_HOST=fake_host
export POSTGRES_PORT=fake_port
export POSTGRES_SSL_MODE=disable
export WRITE_PASSWORD=fake
export EMAIL_USER=fake@mail.com
export EMAIL_PASSWORD=fake

source venv/bin/activate
cicd/test-runner.py
deactivate
