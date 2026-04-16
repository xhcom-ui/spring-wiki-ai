FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/client-app-1.0.0.jar /app/client-app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "client-app.jar"]
