#!/usr/bin/env bash

source venv/bin/activate && cicd/test.py && deactivate
