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
    When A user opens Grafana.
    And The user logs into Grafana as an admin.
    And The user checks that the Air Quality Application dashboards are installed.
    And The user navigates to Air Quality Data Monitor dashboard.
    And The user navigates to Air Quality Index (ICA) dashboard.
    And The user navigates to Air Quality - Pollutants dashboard.
    And The user navigates to Air Quality - Particulate matter dashboard.
