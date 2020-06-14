# Random Menu Selector

## Prerequisites
- [Docker](https://www.docker.com)
- [Python3.7 & PIP](https://www.python.org/downloads)
- Mac users
  - [Homebrew](https://brew.sh)
- Debian based Linux users
  - [Apt](https://en.wikipedia.org/wiki/APT_(software))
  
Platforms other than **_Mac_** and **_Debian based Linux_** are not supported 

## Run tests
```
# Needs to be ran only once
$ cicd/setup.sh

# The test command to run all tests
$ cicd/venv/bin/python cicd/ci.py -c ';clean ;test'
```

## Development
On top of the **_Prerequisites_**, **_JDK 8_** and **_SBT_** is required

When debugging with **_Intellij_**, make sure
- For Unit Tests (All test files except the `test/IntegrationTest.scala`)
  - Select **_ScalaTest_** and import `cicd/test.env` file from **_EnvFile_** tab in test configurations
- For Integration Tests (The `test/IntegrationTest.scala` file)
  - `$ cicd/venv/bin/python cicd/ci.py -d -c 'testOnly *IntegrationTest -- -z "test_name"'`
  - Select **_Remote Debug_** in test configurations and run until the debugger attaches to the port

