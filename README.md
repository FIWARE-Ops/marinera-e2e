# E2E Tests for the FIWARE platform

This repository contains tests for running e2e-verfication on the FIWARE platform deployed by the [marinera-project](https://github.com/FIWARE-Ops/marinera).

## Tools

The tests are structured using [cucumber](https://cucumber.io/). The feature-definitions can be found in the [test/resources](./src/test/resources/testDefinition/verfiy_data_in_grafana.feature). The test execution and implementation itself
uses [junit5](https://junit.org/junit5/docs/current/user-guide/) and the [okhttp-client](https://square.github.io/okhttp/) for all API-calls. The frontend-checks(especially those to grafana) use [selenium](https://www.selenium.dev/).

## Execute

For executing the tests, a selenium-instance is required. It can f.e. be run via docker:

```shell
  docker run -p 4444:4444 --shm-size="2g" selenium/standalone-chrome
```

Test execution can be done via maven:

```shell
    mvn clean test
```

or by running the docker container:

```shell
    docker run quay.io/fiware/marinera-e2e:<VERSION>
```

## Configuration

The tests can be configured, using the following Enviornment-Variables:

| Name                              | Description                                                           | Default                                   |
|-----------------------------------|-----------------------------------------------------------------------|-------------------------------------------|
| REMOTE_DRIVER_URL                 | Url of the selenium instance to be used.                              | ```http://localhost:4444```               |
| GRAFANA_URL                       | Url of the grafana to check.                                          | ```http://localhost:3000```               |
| BROKER_URL                        | Url of the broker to be used for data creation.                       | ```http://localhost:1026```               |
| QUANTUM_LEAP_URL                  | Url of the quantum-leap to receive the notifications from the broker. | ```http://quantumleap-quantumleap:8668``` |
| TEST_ENTITY_ID                    | Id of the test-entity to be created.                                  | ```test-air-quality```                    |
| DATASOURCE_CHECKER_DASHBOARD_NAME | Name of the datasource checker dashboard.                             | ```orion-datasource-checker```            |
| HISTORIC_DATA_GRID_POSITION       | Position of the historic data table inside the data grid.             | ```1```                                   |
| CURRENT_DATA_GRID_POSITION        | Position of the current data table inside the data grid.              | ```2```                                   |
| GRAFANA_USERNAME                  | Username to be used for logging into grafana.                         | ```user```                                |
| GRAFANA_PASSWORD                  | Password to be used for logging into grafana.                         | ```password```                            |

## Security

The tests can run directly through the context-broker api or include testing the [PEP-Proxy](https://github.com/FIWARE/tutorials.PEP-Proxy) by getting a token from [Keycloak](https://www.keycloak.org/) and authenticate with that.
To enable the security integration, configure the following vars:

| Name                               | Description                                           | Default                     |
|------------------------------------|-------------------------------------------------------|-----------------------------|
| PEP_FLOW_ENABLED                   | Enable the test-data flowing through the PEP-Proxy    | ```false```                 |
| KEYCLOAK_USERNAME                  | Username to be used for JWT generation.               | ```null```                  |
| KEYCLOAK_PASSWORD                  | Password to be used for JWT generation.               | ```null```                  |
| KEYCLOAK_CLIENT_ID                 | ClientId to be used for JWT generation.               | ```null```                  |
| KEYCLOAK_CLIENT_SECRET             | ClientSecret to be used for JWT generation.           | ```null```                  |
| KEYCLOAK_URL                       | Url of the keycloak.                                  | ```null```                  |
| KEYCLOAK_REALM                     | Realm to authenticate in.                             | ```null```                  |
| BROKER_URL                         | Url of the broker has to be changed to the PEP-Proxy. | ```http://localhost:1026``` |

Be aware that the user requires the scopes to create and update entities and create subscriptions.