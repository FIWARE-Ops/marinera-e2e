FROM docker.io/library/maven:3.8.5-openjdk-18-slim

COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn dependency:go-offline -B
RUN mvn package -DskipTests