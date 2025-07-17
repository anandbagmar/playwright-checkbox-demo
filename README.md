# playwright-checkbox-demo

## Prerequisites
* Update serverUrl in [eyes-config.yml](src/main/resources/eyes-config.yml)
* Update apiKey in [eyes-config.yml](src/main/resources/eyes-config.yml), OR, provide **APPLITOOLS_API_KEY** as an environment variable.

## Run the test in Sequence

```shell
mvn compile exec:java -Dexec.mainClass=com.example.SequentialTests
```

## Run the test in Parallel

```shell
mvn compile exec:java -Dexec.mainClass=com.example.ParallelTests
```
