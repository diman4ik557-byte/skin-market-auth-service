FROM openjdk:17-jdk-slim

WORKDIR /app
COPY target/auth-service-*.jar app.jar

EXPOSE 9510

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=jwt"]