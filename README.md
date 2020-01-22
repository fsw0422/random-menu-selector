# How to run test

For database integration test, we need to setup dependencies

1. Install [Docker](https://www.docker.com)
2. Install [Python3 & PIP3](https://www.python.org/downloads)
2. **(Mac users only)** [Homebrew](https://brew.sh)
3. Create Python virtual environment and install `test-containers` package and it's dependencies
   - `cicd/setup-test-containers.sh`
4. Build image for build environment
   - `cicd/build-env/build.sh`
   
Finally Run `cicd/ci.sh`

# How to develop

On top of the above dependencies, install **_OpenJdk 8_** and **_SBT_**
