# How to run test

For the Integration test, we need to setup dependencies for database integration

1. Install [Docker](https://www.docker.com)
2. Install [Python3 & PIP3](https://www.python.org/downloads)
3. Install [Postgresql](https://www.postgresql.org) development environment
   - OSX: `brew install postgresql`
   - Ubuntu: `apt install libpq-dev python3-dev`
4. Create Python virtual environment and install dependencies
   - `setup.sh`
5. Build image for build environment
   - `cicd/build-env/build.sh`
   
Finally Run `cicd/ci.sh`


# How to develop

On top of the test dependencies, install **_OpenJdk 8_** and **_SBT_**
