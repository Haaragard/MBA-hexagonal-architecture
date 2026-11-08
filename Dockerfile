FROM gradle:8.2.1-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :infrastructure:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/application.jar /app/application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
