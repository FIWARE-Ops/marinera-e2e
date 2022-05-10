Feature: Verify data in grafana, through the broker.

  Scenario: Login and verify
    Given The subscription between Orion-LD and QuantumLeap is created.
    And Historical data is created.
    When A user opens Grafana.
    And The user logs into Grafana as an admin.
    And The user navigates to the dashboard.