FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

RUN mkdir -p collector-service spring-sdk demo-victim-app

COPY pom.xml .
COPY collector-service/pom.xml collector-service/pom.xml
COPY spring-sdk/pom.xml spring-sdk/pom.xml
COPY demo-victim-app/pom.xml demo-victim-app/pom.xml
COPY spring-sdk/src spring-sdk/src
COPY demo-victim-app/src demo-victim-app/src

RUN mvn -q -pl demo-victim-app -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/demo-victim-app/target/demo-victim-app-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
