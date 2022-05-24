Feature: Data is shown in grafana.

  @unsecured
  Scenario: Create AirQuality data directly through the broker, login to grafana and watch it.
    Given The subscription between Orion-LD and QuantumLeap is created.
    And Some AirQuality data is created.
    When A user opens Grafana.
    And The user logs into Grafana as an admin.
    And The user navigates to the dashboard.
    Then The current air-quality data should be visible.
    And The air-quality data history should be visible.

  @secured
  Scenario: Create AirQuality data through the PEP-Proxy, login to grafana and watch it.
    Given The keycloak connection is setup.
    And The subscription between Orion-LD and QuantumLeap is created.
    And Some AirQuality data is created.
    When A user opens Grafana.
    And The user logs into Grafana as an admin.
    And The user navigates to the dashboard.
    Then The current air-quality data should be visible.
    And The air-quality data history should be visible.
  
  @aqapp
  Scenario: Check that the Air Quality Application is deployed.
    Given Grafana is deployed.
    When A user opens Grafana.
    And The user logs into Grafana as an admin.
    Then The user should be able to navigate to Air Quality Data Monitor dashboard.
    And The user should be able to navigate to Air Quality Index (ICA) dashboard.
    And The user should be able to return to Air Quality Data Monitor dashboard.
    And The user should be able to navigate to Air Quality - Pollutants dashboard.
    And The user should be able to return to Air Quality Data Monitor dashboard.
    And The user should be able to navigate to Air Quality - Particulate matter dashboard.
