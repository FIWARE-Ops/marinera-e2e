# E2E Tests for the FIWARE platform

This repository contains tests for running e2e-verfication on the FIWARE platform deployed by the [marinera-project](https://github.com/FIWARE-Ops/marinera).

## Tools

The tests are structured using [cucumber](https://cucumber.io/). The feature-definitions can be found in the [test/resources](./src/test/resources/testDefinition/verfiy_data_in_grafana.feature).
The test execution and implementation itself uses [junit5](https://junit.org/junit5/docs/current/user-guide/) and the [okhttp-client](https://square.github.io/okhttp/) for all API-calls. 
The frontend-checks(especially those to grafana) use [selenium](https://www.selenium.dev/). 
