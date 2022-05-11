Feature: Verify data in grafana, through the broker.

  Scenario: Create AirQuality data, login to grafana and watch it.
    Given The subscription between Orion-LD and QuantumLeap is created.
    And Some AirQuality data is created.
    When A user opens Grafana.
    And The user logs into Grafana as an admin.
    And The user navigates to the dashboard.
    Then The current air-quality data should be visible.
    And The air-quality data history should be visible.
