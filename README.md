# playwright-checkbox-demo

## Prerequisites
* Update serverUrl in [eyes-config.yml](src/main/resources/eyes-config.yml)
* Update apiKey in [eyes-config.yml](src/main/resources/eyes-config.yml), OR, provide **APPLITOOLS_API_KEY** as an environment variable.
* Update proxy details in [eyes-config.yml](src/main/resources/eyes-config.yml)
* If you want to run the tests with Applitools disabled, in [eyes-config.yml](src/main/resources/eyes-config.yml) set `isDisabled=true` or set the environment variable `DISABLE_APPLITOOLS=true` before running the test 

## Run the test in Sequence

```shell
mvn compile exec:java -Dexec.mainClass=com.example.SequentialTests
```

## Run the test in Parallel

```shell
mvn compile exec:java -Dexec.mainClass=com.example.ParallelTests
```
