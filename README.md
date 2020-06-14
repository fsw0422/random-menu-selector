# Random Menu Selector

## Prerequisites
- [Docker](https://www.docker.com)
- [Python 3.7 & PIP](https://www.python.org/downloads)
- Run the following command
  ```
  $ cicd/setup.sh
  ```

## Run tests
```
$ cicd/ci.sh
```

## Development
In order to develop and test, meet the following dependencies on top of the **_Prerequisites_**
- JDK 8
- SBT 1.2.x

When debugging with **_Intellij_**, make sure
- For Unit Tests (All test files except the `test/IntegrationTest.scala`)
  - Select **_ScalaTest_** and import `cicd/test.env` file from **_EnvFile_** tab in test configurations
- For Integration Tests (The `test/IntegrationTest.scala` file)
  - Run the following command
    ```
    $ cicd/venv/bin/python cicd/test.py -d -c 'testOnly *IntegrationTest -- -z "test_name"'
    ```
  - Select **_Remote Debug_** in test configurations and run until the debugger attaches to the port

