FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system claims && useradd --system --gid claims claims
WORKDIR /app
COPY --from=build /workspace/target/claims-0.0.1-SNAPSHOT.jar app.jar
USER claims
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
