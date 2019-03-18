#!/bin/bash

sbt ";clean ;scapegoat ;coverage ;test ;coverageReport"
