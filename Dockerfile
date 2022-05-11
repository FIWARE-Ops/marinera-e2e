FROM docker.io/library/maven:3.8.5-openjdk-18-slim

ENV REMOTE_DRIVER_URL="http://selenium-hub:4444"
ENV GRAFANA_URL="https://grafana-demo.apps.fiware-spain.emea-1.rht-labs.com"
ENV BROKER_URL="http://orion-ld:1026"
ENV QUANTUM_LEAP_URL="http://quantumleap-quantumleap:8668"
ENV TEST_ENTITY_ID="test-air-quality"
ENV DATASOURCE_CHECKER_DASHBOARD_NAME="orion-datasource-checker"
ENV HISTORIC_DATA_GRID_POSITION="1"
ENV CURRENT_DATA_GRID_POSITION="2"
ENV GRAFANA_USERNAME="user"
ENV GRAFANA_PASSWORD="password"

COPY pom.xml /opt/e2e/
COPY src /opt/e2e/src/

WORKDIR /opt/e2e

RUN mvn dependency:go-offline -B
RUN mvn package -DskipTests

CMD ["mvn", "test"]